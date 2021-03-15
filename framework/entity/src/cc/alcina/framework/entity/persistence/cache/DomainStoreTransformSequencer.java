package cc.alcina.framework.entity.persistence.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;
import cc.alcina.framework.entity.util.SqlUtils;

/**
 * 
 * 
 * A postgres-specific class to order applications of transformrequests to the domain by db transactionCommitTime. 
 * It uses a null-specific index and pg_xact_commit_timestamp(xmin) to order these efficiently - 
 * 
 * @author nick@alcina.cc
 * 
 * @formatter:off
 * 
 CREATE INDEX CONCURRENTLY domaintransformrequest_transactionCommitTime_null1
									  ON domaintransformrequest  USING btree(transactionCommitTime) WHERE transactionCommitTime IS NULL
	
explain analyze select id, pg_xact_commit_timestamp(xmin) as commit_timestamp 
						from domaintransformrequest where transactionCommitTime is null 	
	
	@formatter:on
 *
 */
public class DomainStoreTransformSequencer
		implements DomainTransformPersistenceQueue.Sequencer {
	static long lastNonConcurrentIndexCreationTime;

	private DomainStoreLoaderDatabase loaderDatabase;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Connection connection;

	private Map<Long, Boolean> publishedIds = new ConcurrentHashMap<>();

	DomainTransformCommitPosition highestVisiblePosition;

	List<DomainTransformCommitPosition> unpublishedPositions = new ArrayList<>();

	Map<Long, DomainTransformCommitPosition> visiblePositions = new LinkedHashMap<>();

	LocalDateTime lastEnsure = null;

	private int serverCount = 1;

	private int serverOffset = 0;

	private volatile boolean initialised = false;

	private ClusteredSequencing clusteredSequencing = Registry
			.impl(ClusteredSequencing.class);

	DomainStoreTransformSequencer(DomainStoreLoaderDatabase loaderDatabase) {
		this.loaderDatabase = loaderDatabase;
	}

	@Override
	public void addIncomingPositions(
			List<DomainTransformCommitPosition> positions) {
		positions.removeIf(p -> publishedIds.containsKey(p.commitRequestId));
		if (positions.size() > 0) {
			logger.info("Publishing unpublished positions:\n{}", positions);
			publishUnpublishedPositions(positions, false);
		}
	}

	@Override
	public Long getLastRequestIdAtTimestamp(Timestamp timestamp) {
		return runWithConnection("getLastRequestId",
				conn -> getRequestIdAtTimestamp0(conn, timestamp));
	}

	public int getServerCount() {
		return this.serverCount;
	}

	public int getServerOffset() {
		return this.serverOffset;
	}

	public boolean isInitialised() {
		return this.initialised;
	}

	@Override
	public void onPersistedRequestCommitted(long requestId) {
		if (highestVisiblePosition == null) {
			return;
		}
		refreshPositions(requestId, System.currentTimeMillis());
	}

	@Override
	public void refresh() {
		refreshPositions(-1, System.currentTimeMillis());
	}

	public void rotateIndex() {
		new IndexRotater().rotate();
	}

	public void setInitialised(boolean initialised) {
		this.initialised = initialised;
	}

	public void setServerCount(int serverCount) {
		this.serverCount = serverCount;
	}

	public void setServerOffset(int serverOffset) {
		this.serverOffset = serverOffset;
	}

	private int ensureTimestamps() throws SQLException {
		return runWithConnection("ensureTimestamps", this::ensureTimestamps0);
	}

	private int ensureTimestamps0(Connection conn) throws SQLException {
		String tableName = tableName();
		String querySql = Ax.format(
				"select id, pg_xact_commit_timestamp(xmin) as commit_timestamp "
						+ "from %s where transactionCommitTime is null FOR UPDATE SKIP LOCKED",
				tableName);
		Map<Long, Timestamp> toUpdate = new LinkedHashMap<>();
		try (PreparedStatement pStatement = conn.prepareStatement(querySql)) {
			pStatement.setFetchSize(1000);
			ResultSet rs = pStatement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong(1);
				Timestamp timestamp = rs.getTimestamp(2);
				toUpdate.put(id, timestamp);
			}
			rs.close();
		}
		String updateSql = Ax.format(
				"update %s set transactionCommitTime=? where id=?", tableName);
		try (PreparedStatement preparedStatement = conn
				.prepareStatement(updateSql)) {
			Set<Entry<Long, Timestamp>> entrySet = toUpdate.entrySet();
			for (Entry<Long, Timestamp> entry : toUpdate.entrySet()) {
				long id = entry.getKey();
				Timestamp timestamp = entry.getValue();
				preparedStatement.setTimestamp(1, timestamp);
				preparedStatement.setLong(2, id);
				preparedStatement.addBatch();
			}
			int[] affectedRecords = preparedStatement.executeBatch();
			logger.trace("Updated {} timestamps", toUpdate.size());
		}
		lastEnsure = LocalDateTime.now();
		return toUpdate.size();
	}

	private Connection getConnection() throws SQLException {
		if (connection == null) {
			connection = loaderDatabase.dataSource.getConnection();
			connection.setAutoCommit(false);
		}
		return connection;
	}

	private Long getRequestIdAtTimestamp0(Connection conn, Timestamp timestamp)
			throws SQLException {
		try (Statement statement = conn.createStatement()) {
			Class<? extends DomainTransformRequestPersistent> persistentClass = loaderDatabase.domainDescriptor
					.getDomainTransformRequestPersistentClass();
			String tableName = persistentClass.getAnnotation(Table.class)
					.name();
			try (PreparedStatement idStatement = conn.prepareStatement(Ax
					.format("select id from %s where transactionCommitTime=? order by id desc limit 1",
							tableName))) {
				idStatement.setTimestamp(1, timestamp);
				ResultSet idRs = idStatement.executeQuery();
				if (idRs.next()) {
					return idRs.getLong(1);
				} else {
					return null;
				}
			}
		}
	}

	private synchronized void publishUnpublishedPositions(
			List<DomainTransformCommitPosition> positions,
			boolean publishToCluster) {
		positions.removeIf(p -> publishedIds.containsKey(p.commitRequestId));
		unpublishedPositions.addAll(positions);
		if (unpublishedPositions.isEmpty()) {
			return;
		}
		loaderDatabase.getStore().getPersistenceEvents().getQueue()
				.onSequencedCommitPositions(unpublishedPositions,
						publishToCluster
								&& loaderDatabase.getStore().isWritable());
		unpublishedPositions
				.forEach(p -> publishedIds.put(p.commitRequestId, true));
		highestVisiblePosition = Ax.last(unpublishedPositions);
		unpublishedPositions.clear();
		synchronized (unpublishedPositions) {
			unpublishedPositions.notifyAll();
		}
	}

	private void refreshPositions(long ignoreIfSeenRequestId,
			long refreshTime) {
		if (!initialised) {
			return;
		}
		if (clusteredSequencing.isPrimarySequenceRefresher()
				&& loaderDatabase.getStore().isWritable()) {
		} else {
			if (publishedIds.containsKey(ignoreIfSeenRequestId)) {
				logger.info(
						"Non-primary sequence refresher: OK (updated)(1): {}",
						ignoreIfSeenRequestId);
				return;
			}
			synchronized (unpublishedPositions) {
				try {
					unpublishedPositions.wait(clusteredSequencing
							.waitForPrimarySequenceRefresherTime());
				} catch (InterruptedException e) {
				}
			}
			if (publishedIds.containsKey(ignoreIfSeenRequestId)) {
				logger.info(
						"Non-primary sequence refresher: OK (updated)(2): {}",
						ignoreIfSeenRequestId);
				return;
			}
			logger.info(
					"Non-primary sequence refresher: non-optimised (not updated): {}",
					ignoreIfSeenRequestId);
		}
		runWithConnection("refresh-positions",
				conn -> refreshPositions0(conn, false, ignoreIfSeenRequestId));
	}

	private synchronized int refreshPositions0(Connection conn, boolean initial,
			long ignoreIfSeenRequestId) throws SQLException {
		if (publishedIds.containsKey(ignoreIfSeenRequestId) && !initial) {
			return 0;
		}
		boolean publishToQueue = !initial;
		String tableName = tableName();
		if (initial) {
			String querySql = Ax.format(
					"select max(transactionCommitTime)  "
							+ "from %s where transactionCommitTime is not null",
					tableName);
			try (PreparedStatement pStatement = conn
					.prepareStatement(querySql)) {
				ResultSet rs = pStatement.executeQuery();
				if (rs.next() && rs.getTimestamp(1) != null) {
					highestVisiblePosition = new DomainTransformCommitPosition(
							0L, rs.getTimestamp(1));
				} else {
					highestVisiblePosition = new DomainTransformCommitPosition(
							0, new Timestamp(0));
					logger.warn(
							"initialising timestamps for store {} - {}/transactionCommitTime",
							loaderDatabase.getStore().name, tableName,
							highestVisiblePosition);
				}
				logger.info(
						"initialised timestamps for store {} - {}/transactionCommitTime",
						loaderDatabase.getStore().name, tableName,
						highestVisiblePosition);
			}
		}
		Timestamp highestVisible = highestVisiblePosition.commitTimestamp;
		/*
		 * Go back a little, to handle concurrent writes to
		 * transactionCommitTime
		 */
		Timestamp since = new Timestamp(highestVisible.getTime() - 1000);
		since.setNanos(highestVisible.getNanos());
		String querySql = Ax.format(
				"select id, transactionCommitTime,pg_xact_commit_timestamp(xmin) as commit_timestamp "
						+ "from %s where transactionCommitTime is null OR"
						+ " transactionCommitTime>=?",
				tableName);
		if (!initial) {
			querySql += " order by pg_xact_commit_timestamp(xmin) desc ";
		}
		List<DomainTransformCommitPosition> positions = new ArrayList<>();
		try (PreparedStatement pStatement = conn.prepareStatement(querySql)) {
			pStatement.setFetchSize(10000);
			pStatement.setTimestamp(1, since);
			ResultSet rs = pStatement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong(1);
				Timestamp storedTimestamp = rs.getTimestamp(2);
				Timestamp xminTimestamp = rs.getTimestamp(3);
				Timestamp timestamp = storedTimestamp != null ? storedTimestamp
						: xminTimestamp;
				if (timestamp.compareTo(since) >= 0) {
					DomainTransformCommitPosition position = new DomainTransformCommitPosition(
							id, timestamp);
					DomainTransformCommitPosition existing = visiblePositions
							.get(position.commitRequestId);
					if (existing != null) {
						if (!existing.commitTimestamp.equals(timestamp)) {
							logger.warn(
									"Different timestamps for positions:\nDB: {}\nExisting: {}",
									timestamp, existing);
						}
					} else {
						positions.add(position);
					}
				}
			}
		} catch (SQLException sqlex) {
			logger.warn("Issue in query: since: {}", since);
			throw sqlex;
		}
		positions.forEach(position -> visiblePositions
				.put(position.commitRequestId, position));
		positions.sort(Comparator.naturalOrder());
		unpublishedPositions.addAll(positions);
		if (positions.size() > 0) {
			logger.trace("Added unpublished positions: - since: {} - {}", since,
					CommonUtils.joinWithNewlines(positions));
		}
		if (positions.size() > 0) {
			if (publishToQueue) {
				publishUnpublishedPositions(positions, true);
			} else {
				unpublishedPositions.addAll(positions);
				highestVisiblePosition = Ax.last(unpublishedPositions);
			}
			if (shouldEnsure()) {
				conn.commit();
				ensureTimestamps();
			}
		}
		return positions.size();
	}

	private <T> T runWithConnection(String metricName,
			ThrowingFunction<Connection, T> connectionProcessor) {
		Connection conn = null;
		String key = Ax.format("dts-%s", metricName);
		try {
			// MetricLogging.get().start(key);
			conn = getConnection();
			return connectionProcessor.apply(conn);
		} catch (Exception e) {
			logger.warn("Exception in connection processor", e);
			return null;
		} finally {
			try {
				conn.commit();
			} catch (SQLException e) {
				e.printStackTrace();
				try {
					logger.warn("Closing connection", e);
					conn.close();
				} catch (SQLException e2) {
					e2.printStackTrace();
				} finally {
					connection = null;
				}
			} finally {
				// MetricLogging.get().end(key);
			}
		}
	}

	private boolean shouldEnsure() {
		if (ChronoUnit.SECONDS.between(lastEnsure, LocalDateTime.now()) < 1) {
			return false;
		}
		if (loaderDatabase.getStore() != DomainStore.writableStore()) {
			return false;
		}
		/*
		 * round-robin, based on server count
		 */
		return clusteredSequencing.isPrimarySequenceRefresher()
				|| LocalDateTime.now().getSecond()
						% serverCount == serverOffset;
	}

	private String tableName() {
		Class<? extends DomainTransformRequestPersistent> persistentClass = loaderDatabase.domainDescriptor
				.getDomainTransformRequestPersistentClass();
		String tableName = persistentClass.getAnnotation(Table.class).name();
		return tableName;
	}

	void initialEnsureTimestamps() throws SQLException {
		ensureTimestamps();
	}

	void markHighestVisibleTransformList(Connection conn) throws SQLException {
		refreshPositions0(conn, true, -1);
		logger.info("Marked highest visible position - {}",
				highestVisiblePosition);
		unpublishedPositions.clear();
	}

	/*
	 * Prevent contentious herds
	 */
	@RegistryLocation(registryPoint = ClusteredSequencing.class, implementationType = ImplementationType.INSTANCE)
	public static class ClusteredSequencing {
		public boolean isPrimarySequenceRefresher() {
			return true;
		}

		public long waitForPrimarySequenceRefresherTime() {
			return ResourceUtilities.getLong(
					DomainStoreTransformSequencer.class,
					"waitForPrimarySequenceRefresherTime");
		}
	}

	class IndexRotater {
		private static final int MAX_INDEX = 3;

		int counter;

		List<Integer> existing;

		List<Integer> drop;

		Integer responseTimeOk;

		Integer created;

		int indexCreationCount;

		public void rotate() {
			existing = new ArrayList<>();
			if (valid()) {
				return;
			}
			for (counter = -1; counter <= MAX_INDEX; counter++) {
				if (exists()) {
					existing.add(counter);
				}
			}
			if (indexCreationCount <= 1) {
				drop = existing;
				created = null;
				for (counter = -1; counter <= MAX_INDEX; counter++) {
					if (!exists()) {
						if (System.currentTimeMillis()
								- lastNonConcurrentIndexCreationTime < 30
										* TimeConstants.ONE_MINUTE_MS) {
							logger.warn(
									"Not creating new index - last creation too recent");
							return;
						}
						create(indexCreationCount == 0);
						indexCreationCount++;
						created = counter;
						break;
					}
				}
				if (created == null) {
					throw new RuntimeException("No blank index slot");
				}
				for (counter = -1; counter <= MAX_INDEX; counter++) {
					if (drop.contains(counter)) {
						drop();
					}
				}
				drop = null;
				rotate();
				return;
			} else {
				throw new RuntimeException("No responseTimeOk after create");
			}
		}

		private void create(boolean concurrently) {
			String concurrentlyWarnString = concurrently ? ""
					: "non-concurrently ";
			logger.warn("Creating index {}{}", concurrentlyWarnString, name());
			try {
				try (Connection conn = loaderDatabase.dataSource
						.getConnection()) {
					Statement statement = conn.createStatement();
					statement.setQueryTimeout(ResourceUtilities.getInteger(
							DomainStoreTransformSequencer.class,
							"indexCreationTimeout"));
					String killIdleInTransactionSql = "SELECT "
							+ "   pg_terminate_backend(pid)  " + " FROM  "
							+ " pg_stat_activity " + " WHERE  "
							// + " -- don't kill my own connection! "
							+ " pid <> pg_backend_pid() "
							// + " -- don't kill the connections to other
							// databases "
							+ " AND state='idle in transaction';";
					SqlUtils.execute(statement, killIdleInTransactionSql);
					String concurrentlyString = concurrently ? "CONCURRENTLY"
							: "";
					SqlUtils.execute(statement, Ax.format("CREATE INDEX  %s "
							+ " %s "
							+ "  ON %s  USING btree(transactionCommitTime) "
							+ " WITH (FILLFACTOR=50) "
							+ "WHERE transactionCommitTime IS NULL" + " ",
							concurrentlyString, name(), tableName()));
					if (!concurrently) {
						lastNonConcurrentIndexCreationTime = System
								.currentTimeMillis();
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private void drop() {
			logger.warn("Dropping index {}", name());
			runWithConnection("drop", conn -> {
				try (Statement statement = conn.createStatement()) {
					String sql = Ax.format("drop index %s", name());
					SqlUtils.execute(statement, sql);
					return null;
				}
			});
		}

		private boolean exists() {
			return runWithConnection("exists", conn -> {
				try (Statement statement = conn.createStatement()) {
					String sql = Ax.format(
							"select * from pg_class where relname='%s'",
							name());
					ResultSet rs = statement.executeQuery(sql);
					return rs.next();
				}
			});
		}

		private String name() {
			String suffix = counter == -1 ? "" : String.valueOf(counter);
			return "domaintransformrequest_transactioncommittime_null" + suffix;
		}

		private boolean valid() {
			return runWithConnection("valid", conn -> {
				String querySql = Ax.format(
						"select id, transactionCommitTime,pg_xact_commit_timestamp(xmin) as commit_timestamp "
								+ "from %s where transactionCommitTime is null OR"
								+ " transactionCommitTime>=?",
						tableName());
				querySql += " order by pg_xact_commit_timestamp(xmin) desc ";
				try (PreparedStatement statement = conn
						.prepareStatement(querySql)) {
					Timestamp since = new Timestamp(System.currentTimeMillis()
							- TimeConstants.ONE_MINUTE_MS);
					statement.setTimestamp(1, since);
					long nanoTime = System.nanoTime();
					Ax.out(querySql);
					statement.execute();
					long nanoDiff = System.nanoTime() - nanoTime;
					if (nanoDiff > 2000000) {
						logger.warn("transactionCommitTime >2ms - {} ns",
								nanoDiff);
						try (PreparedStatement statement2 = conn
								.prepareStatement(
										"explain analyze " + querySql)) {
							statement2.setTimestamp(1, since);
							ResultSet rs = statement2.executeQuery();
							while (rs.next()) {
								logger.warn(rs.getString(1));
							}
						}
					}
					// FIXME - 2022 - moving average? for the moment, 7ms
					// (although should be <1)
					return nanoDiff < 7000000;
				}
			});
		}
	}
}

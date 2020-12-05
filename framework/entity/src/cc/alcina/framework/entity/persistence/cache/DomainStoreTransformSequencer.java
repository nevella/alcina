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

import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;

/**
 * dtrp - start persist time (set in persister), committime
 * 
 * and that's all she wrote. updater sets persist/committime to update time,
 * 
 * TODO - this really bridges the db side (and in the right package for that)
 * and domain transform queues - would be nice to split in two and reduce
 * visibility of dtrq package methods. And re-implement as subclass
 * (commit/dbcommit) sequencing.
 * 
 * A note re the complexity of the interaction of this class and
 * DomainTransformPersistenceQueue:: most of the complexity is because we can't
 * know who will call
 * DomainTransformPersistenceEvents.fireDomainTransformPersistenceEvent() first
 * :: the originator of the transforms, or the firing queue. So rather than do
 * any general initialisation, each thread performs wait/notify loops on the
 * maps containing the per-thread barriers to ensure that the thread requiring
 * the barrier has generated it before proceeding.
 * 
 * @author nick@alcina.cc
 *
 */
public class DomainStoreTransformSequencer
		implements DomainTransformPersistenceQueue.Sequencer {
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

	DomainStoreTransformSequencer(DomainStoreLoaderDatabase loaderDatabase) {
		this.loaderDatabase = loaderDatabase;
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
		refreshPositions(requestId);
	}

	@Override
	public void refresh() {
		refreshPositions(-1);
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
			logger.info("Updated {} timestamps", toUpdate.size());
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

	private void refreshPositions(long ignoreIfSeenRequestId) {
		if (!initialised) {
			return;
		}
		runWithConnection("refresh-positions",
				conn -> refreshPositions0(conn, false, ignoreIfSeenRequestId));
	}

	private synchronized int refreshPositions0(Connection conn, boolean initial,
			long ignoreIfSeenRequestId) throws SQLException {
		if (publishedIds.containsKey(ignoreIfSeenRequestId)) {
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
				if (rs.next()) {
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
			querySql += " order by pg_xact_commit_timestamp(xmin) desc limit 100";
		}
		List<DomainTransformCommitPosition> positions = new ArrayList<>();
		try (PreparedStatement pStatement = conn.prepareStatement(querySql)) {
			pStatement.setFetchSize(100);
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
						visiblePositions.put(position.commitRequestId,
								position);
					}
				}
			}
		}
		positions.sort(Comparator.naturalOrder());
		unpublishedPositions.addAll(positions);
		if (positions.size() > 0) {
			logger.info("Added unpublished positions: {}",
					CommonUtils.joinWithNewlines(positions));
		}
		if (positions.size() > 0) {
			highestVisiblePosition = Ax.last(unpublishedPositions);
		}
		if (publishToQueue) {
			loaderDatabase.getStore().getPersistenceEvents().getQueue()
					.onSequencedCommitPositions(unpublishedPositions);
			unpublishedPositions
					.forEach(p -> publishedIds.put(p.commitRequestId, true));
			unpublishedPositions.clear();
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
			MetricLogging.get().start(key);
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
					conn = null;
				}
			} finally {
				MetricLogging.get().end(key);
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
		return LocalDateTime.now().getSecond() % serverCount == serverOffset;
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
}

package cc.alcina.framework.entity.persistence.domain;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.persistence.Table;

import org.slf4j.Logger;

import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;
import cc.alcina.framework.entity.util.OffThreadLogger;

/**
 * 
 * 
 * A postgres-specific class to order applications of transformrequests to the
 * domain by db transactionCommitTime. It uses pg_xact_commit_timestamp(xmin) to
 * order these correctly.
 * 
 * @author nick@alcina.cc
 * 
 *
 */
public class DomainStoreTransformSequencer
		implements DomainTransformPersistenceQueue.Sequencer {
	private DomainStoreLoaderDatabase loaderDatabase;

	private Logger logger = OffThreadLogger.getLogger(getClass());

	private Connection connection;

	private ConcurrentHashMap<Long, Boolean> publishedIds = new ConcurrentHashMap<>();

	/*
	 * Synchronization - this is iterated over in refreshPositions0, but a
	 * concurrent add will not cause problems (since the ultimate concurrency
	 * control is the db-tx visibility of the dtr id)
	 */
	private ConcurrentHashMap<Long, Boolean> pendingRequestIds = new ConcurrentHashMap<>();

	private ConcurrentHashMap<Long, Boolean> abortedRequestIds = new ConcurrentHashMap<>();

	DomainTransformCommitPosition highestVisiblePosition;

	List<DomainTransformCommitPosition> unpublishedPositions = new ArrayList<>();

	Map<Long, DomainTransformCommitPosition> visiblePositions = new LinkedHashMap<>();

	private volatile boolean initialised = false;

	DomainStoreTransformSequencer(DomainStoreLoaderDatabase loaderDatabase) {
		this.loaderDatabase = loaderDatabase;
		highestVisiblePosition = new DomainTransformCommitPosition(0,
				new Timestamp(0));
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
	public void onPersistedRequestAborted(long requestId) {
		pendingRequestIds.remove(requestId);
		logger.info("Received aborted request id: {}", requestId);
		abortedRequestIds.put(requestId, true);
	}

	@Override
	public void onPersistedRequestPreCommitted(long requestId) {
		if (abortedRequestIds.containsKey(requestId)) {
			logger.info("Received precommit after aborted request - id: {}",
					requestId);
			return;
		}
		pendingRequestIds.put(requestId, true);
	}

	@Override
	public void refresh() {
		refreshPositions(-1, System.currentTimeMillis());
	}

	public void setInitialised(boolean initialised) {
		this.initialised = initialised;
	}

	@Override
	public void vacuumTables() {
		runWithConnection("vacuum", this::vacuumTables0);
	}

	private Connection getConnection() throws SQLException {
		if (connection == null) {
			connection = loaderDatabase.dataSource.getConnection();
			connection.setAutoCommit(false);
		}
		return connection;
	}

	private synchronized void publishUnpublishedPositions(
			List<DomainTransformCommitPosition> positions) {
		positions.removeIf(
				p -> publishedIds.containsKey(p.getCommitRequestId()));
		unpublishedPositions.addAll(positions);
		if (unpublishedPositions.isEmpty()) {
			return;
		}
		loaderDatabase.getStore().getPersistenceEvents().getQueue()
				.onSequencedCommitPositions(unpublishedPositions);
		unpublishedPositions
				.forEach(p -> pendingRequestIds.remove(p.getCommitRequestId()));
		unpublishedPositions
				.forEach(p -> publishedIds.put(p.getCommitRequestId(), true));
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
		runWithConnection("refresh-positions",
				conn -> refreshPositions0(conn, ignoreIfSeenRequestId));
	}

	private synchronized int refreshPositions0(Connection conn,
			long ignoreIfSeenRequestId) throws SQLException {
		if (publishedIds.containsKey(ignoreIfSeenRequestId)) {
			return 0;
		}
		String tableName = tableName();
		long start = System.nanoTime();
		Timestamp highestVisible = highestVisiblePosition.getCommitTimestamp();
		String querySql = null;
		querySql = Ax.format(
				"select id, pg_xact_commit_timestamp(xmin) as commit_timestamp "
						+ "from %s where id in %s order by pg_xact_commit_timestamp(xmin) desc ",
				tableName,
				EntityPersistenceHelper.toInClause(pendingRequestIds.keySet()));
		List<DomainTransformCommitPosition> positions = new ArrayList<>();
		try (Statement statement = conn.createStatement()) {
			statement.setFetchSize(10000);
			ResultSet rs = statement.executeQuery(querySql);
			while (rs.next()) {
				long id = rs.getLong(1);
				Timestamp xminTimestamp = rs.getTimestamp(2);
				DomainTransformCommitPosition position = new DomainTransformCommitPosition(
						id, xminTimestamp);
				DomainTransformCommitPosition existing = visiblePositions
						.get(position.getCommitRequestId());
				if (existing != null) {
					pendingRequestIds.remove(existing.getCommitRequestId());
				} else {
					positions.add(position);
				}
			}
			rs.close();
		} catch (SQLException sqlex) {
			logger.warn("Issue in query: ids: {}", pendingRequestIds.keySet());
			throw sqlex;
		}
		positions.forEach(position -> visiblePositions
				.put(position.getCommitRequestId(), position));
		positions.sort(Comparator.naturalOrder());
		unpublishedPositions.addAll(positions);
		if (positions.size() > 0) {
			logger.trace("Added unpublished positions: -  - {}",
					CommonUtils.joinWithNewlines(positions));
		}
		long end = System.nanoTime();
		if (end - start > ResourceUtilities.getInteger(
				DomainStoreTransformSequencer.class, "logRefreshTime")) {
			logger.warn("Long refresh time: {} ids - {} ns - {}",
					pendingRequestIds.size(), end - start,
					pendingRequestIds.keySet().stream().limit(20)
							.map(String::valueOf)
							.collect(Collectors.joining(", ")));
		}
		if (positions.size() > 0) {
			publishUnpublishedPositions(positions);
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

	private String tableName() {
		Class<? extends DomainTransformRequestPersistent> persistentClass = loaderDatabase.domainDescriptor
				.getDomainTransformRequestPersistentClass();
		String tableName = persistentClass.getAnnotation(Table.class).name();
		return tableName;
	}

	private int vacuumTables0(Connection conn) throws SQLException {
		String querySql = Ax.format("vacuum (VERBOSE, ANALYZE) %s ",
				tableName());
		try (Statement statement = conn.createStatement()) {
			ResultSet rs = statement.executeQuery(querySql);
			while (rs.next()) {
				logger.info(rs.getString(1));
			}
		} catch (SQLException sqlex) {
			logger.warn("Issue in vacuum");
			throw sqlex;
		}
		return 0;
	}

	void markHighestVisibleTransformList(Connection conn) throws SQLException {
		if (!isEnabled()) {
			highestVisiblePosition = new DomainTransformCommitPosition(0L,
					new Timestamp(0L));
			return;
		}
		refreshPositions0(conn, -1);
		logger.info("Marked highest visible position - {}",
				highestVisiblePosition);
		unpublishedPositions.clear();
	}

	private boolean isEnabled() {
		return loaderDatabase.domainDescriptor.isUsesCommitSequencer();
	}

	void waitForWritableTransactionsToTerminate() throws SQLException {
		if (!isEnabled()) {
			return;
		}
		runWithConnection("ensureTimestamps",
				this::waitForWritableTransactionsToTerminate0);
	}

	private long waitForWritableTransactionsToTerminate0(Connection conn)
			throws SQLException, InterruptedException {
		//@formatter:off
		String sql =""+
		"SELECT 	* " +
		"	FROM " +
		"	    pg_stat_activity " +
		"	WHERE " +
		"	    backend_xid is not null" +
		"	    AND xact_start < ? " +
		"	    AND pid <> pg_backend_pid()" +
		"	    AND state <> 'idle';	  " ;
		//@formatter:on
		long start = System.currentTimeMillis();
		PreparedStatement statement = conn.prepareStatement(sql);
		while (true) {
			statement.setTimestamp(1, new Timestamp(start));
			conn.commit();
			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					break;
				}
				logger.info("Waiting on transactions:");
				do {
					logger.info(
							"\tpid: {} - client_addr: {} - xact_start: {} - query: {}",
							rs.getLong("pid"), rs.getString("client_addr"),
							rs.getString("xact_start"), rs.getString("query"));
				} while (rs.next());
			}
			Thread.sleep(1000);
		}
		return System.currentTimeMillis() - start;
	}
}

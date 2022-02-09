package cc.alcina.framework.entity.persistence.domain;

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
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceQueue;
import cc.alcina.framework.entity.util.OffThreadLogger;
import cc.alcina.framework.common.client.logic.reflection.Registration;

/**
 *  A postgres-specific class to order applications of transformrequests to the domain by db transactionCommitTime.
 *  It uses a null-specific index and pg_xact_commit_timestamp(xmin) to order these efficiently -
 *
 *  @author nick@alcina.cc
 *
 *  @formatter:off
 *
 *  CREATE INDEX CONCURRENTLY domaintransformrequest_transactionCommitTime_null1
 * 									  ON domaintransformrequest  USING btree(transactionCommitTime) WHERE transactionCommitTime IS NULL
 *
 * explain analyze select id, pg_xact_commit_timestamp(xmin) as commit_timestamp
 * 						from domaintransformrequest where transactionCommitTime is null
 *
 * 	 CREATE INDEX CONCURRENTLY domaintransformrequest_transactionCommitTime_nullsfirst
 * 									  ON domaintransformrequest  USING btree(transactionCommitTime DESC NULLS FIRST)
 *
 * 	@formatter:on
 *
 * 	FIXME - document - the above index is only used on store initialisation
 *
 * 	FIXME - 2022 - no it definitely is not. But it probably should be -
 * 	since we have guaranteed ids either at the cluster level or sole-server, we can
 * 	filter the update by those ids (so no need for an index) - except in the recovery case
 */
public class DomainStoreTransformSequencer implements DomainTransformPersistenceQueue.Sequencer {

    static long lastNonConcurrentIndexCreationTime;

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

    DomainTransformCommitPosition highestVisiblePosition;

    List<DomainTransformCommitPosition> unpublishedPositions = new ArrayList<>();

    Map<Long, DomainTransformCommitPosition> visiblePositions = new LinkedHashMap<>();

    LocalDateTime lastEnsure = null;

    private volatile boolean initialised = false;

    private ClusteredSequencing clusteredSequencing = Registry.impl(ClusteredSequencing.class);

    DomainStoreTransformSequencer(DomainStoreLoaderDatabase loaderDatabase) {
        this.loaderDatabase = loaderDatabase;
    }

    @Override
    public Long getLastRequestIdAtTimestamp(Timestamp timestamp) {
        return runWithConnection("getLastRequestId", conn -> getRequestIdAtTimestamp0(conn, timestamp));
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
    public void onPersistedRequestPreCommitted(long requestId) {
        pendingRequestIds.put(requestId, true);
    }

    @Override
    public void refresh() {
        refreshPositions(-1, System.currentTimeMillis());
    }

    public void setInitialised(boolean initialised) {
        this.initialised = initialised;
    }

    public void vacuumTables() {
        runWithConnection("vacuum", this::vacuumTables0);
    }

    private int ensureTimestamps() throws SQLException {
        if (!isEnabled()) {
            return 0;
        }
        return runWithConnection("ensureTimestamps", this::ensureTimestamps0);
    }

    private synchronized int ensureTimestamps0(Connection conn) throws SQLException {
        String tableName = tableName();
        String querySql = Ax.format("select id, pg_xact_commit_timestamp(xmin) as commit_timestamp " + "from %s where transactionCommitTime is null FOR UPDATE SKIP LOCKED", tableName);
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
        String updateSql = Ax.format("update %s set transactionCommitTime=? where id=?", tableName);
        try (PreparedStatement preparedStatement = conn.prepareStatement(updateSql)) {
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

    private Long getRequestIdAtTimestamp0(Connection conn, Timestamp timestamp) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            Class<? extends DomainTransformRequestPersistent> persistentClass = loaderDatabase.domainDescriptor.getDomainTransformRequestPersistentClass();
            String tableName = persistentClass.getAnnotation(Table.class).name();
            try (PreparedStatement idStatement = conn.prepareStatement(Ax.format("select id from %s where transactionCommitTime=? order by id desc limit 1", tableName))) {
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

    private synchronized void publishUnpublishedPositions(List<DomainTransformCommitPosition> positions) {
        positions.removeIf(p -> publishedIds.containsKey(p.getCommitRequestId()));
        unpublishedPositions.addAll(positions);
        if (unpublishedPositions.isEmpty()) {
            return;
        }
        loaderDatabase.getStore().getPersistenceEvents().getQueue().onSequencedCommitPositions(unpublishedPositions);
        unpublishedPositions.forEach(p -> pendingRequestIds.remove(p.getCommitRequestId()));
        unpublishedPositions.forEach(p -> publishedIds.put(p.getCommitRequestId(), true));
        highestVisiblePosition = Ax.last(unpublishedPositions);
        unpublishedPositions.clear();
        synchronized (unpublishedPositions) {
            unpublishedPositions.notifyAll();
        }
    }

    private void refreshPositions(long ignoreIfSeenRequestId, long refreshTime) {
        if (!initialised) {
            return;
        }
        runWithConnection("refresh-positions", conn -> refreshPositions0(conn, false, ignoreIfSeenRequestId));
    }

    private synchronized int refreshPositions0(Connection conn, boolean initial, long ignoreIfSeenRequestId) throws SQLException {
        if (publishedIds.containsKey(ignoreIfSeenRequestId) && !initial) {
            return 0;
        }
        boolean publishToQueue = !initial;
        String tableName = tableName();
        if (initial) {
            String querySql = Ax.format("select max(transactionCommitTime)  " + "from %s where transactionCommitTime is not null", tableName);
            try (PreparedStatement pStatement = conn.prepareStatement(querySql)) {
                ResultSet rs = pStatement.executeQuery();
                if (rs.next() && rs.getTimestamp(1) != null) {
                    highestVisiblePosition = new DomainTransformCommitPosition(0L, rs.getTimestamp(1));
                } else {
                    highestVisiblePosition = new DomainTransformCommitPosition(0, new Timestamp(0));
                    logger.warn("initialising timestamps for store {} - {}/transactionCommitTime", loaderDatabase.getStore().name, tableName, highestVisiblePosition);
                }
                logger.info("initialised timestamps for store {} - {}/transactionCommitTime", loaderDatabase.getStore().name, tableName, highestVisiblePosition);
            }
        }
        Timestamp highestVisible = highestVisiblePosition.getCommitTimestamp();
        /*
		 * Go back a little, to handle concurrent writes to
		 * transactionCommitTime
		 */
        Timestamp since = new Timestamp(highestVisible.getTime() - 1000);
        since.setNanos(highestVisible.getNanos());
        String querySql = null;
        if (initial) {
            querySql = Ax.format("select id, transactionCommitTime,pg_xact_commit_timestamp(xmin) as commit_timestamp " + "from %s where transactionCommitTime is null OR" + " transactionCommitTime>=?", tableName);
        } else {
            querySql = Ax.format("select id, transactionCommitTime,pg_xact_commit_timestamp(xmin) as commit_timestamp " + "from %s where id in %s order by pg_xact_commit_timestamp(xmin) desc ", tableName, EntityPersistenceHelper.toInClause(pendingRequestIds.keySet()));
        }
        List<DomainTransformCommitPosition> positions = new ArrayList<>();
        try (PreparedStatement pStatement = conn.prepareStatement(querySql)) {
            pStatement.setFetchSize(10000);
            if (initial) {
                pStatement.setTimestamp(1, since);
            }
            ResultSet rs = pStatement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                Timestamp storedTimestamp = rs.getTimestamp(2);
                Timestamp xminTimestamp = rs.getTimestamp(3);
                Timestamp timestamp = storedTimestamp != null ? storedTimestamp : xminTimestamp;
                if (!initial || timestamp.compareTo(since) >= 0) {
                    DomainTransformCommitPosition position = new DomainTransformCommitPosition(id, timestamp);
                    DomainTransformCommitPosition existing = visiblePositions.get(position.getCommitRequestId());
                    if (existing != null) {
                        if (!existing.getCommitTimestamp().equals(timestamp)) {
                            logger.warn("Different timestamps for positions:\nDB: {}\nExisting: {}", timestamp, existing);
                        }
                        pendingRequestIds.remove(existing.getCommitRequestId());
                    } else {
                        positions.add(position);
                    }
                }
            }
        } catch (SQLException sqlex) {
            logger.warn("Issue in query: since: {}", since);
            throw sqlex;
        }
        positions.forEach(position -> visiblePositions.put(position.getCommitRequestId(), position));
        positions.sort(Comparator.naturalOrder());
        unpublishedPositions.addAll(positions);
        if (positions.size() > 0) {
            logger.trace("Added unpublished positions: - since: {} - {}", since, CommonUtils.joinWithNewlines(positions));
        }
        if (positions.size() > 0) {
            if (publishToQueue) {
                publishUnpublishedPositions(positions);
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

    private <T> T runWithConnection(String metricName, ThrowingFunction<Connection, T> connectionProcessor) {
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
        return clusteredSequencing.isPrimarySequenceRefresher();
    }

    private String tableName() {
        Class<? extends DomainTransformRequestPersistent> persistentClass = loaderDatabase.domainDescriptor.getDomainTransformRequestPersistentClass();
        String tableName = persistentClass.getAnnotation(Table.class).name();
        return tableName;
    }

    private int vacuumTables0(Connection conn) throws SQLException {
        String querySql = Ax.format("vacuum (VERBOSE, ANALYZE) %s ", tableName());
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

    void initialEnsureTimestamps() throws SQLException {
        ensureTimestamps();
    }

    void markHighestVisibleTransformList(Connection conn) throws SQLException {
        if (!isEnabled()) {
            highestVisiblePosition = new DomainTransformCommitPosition(0L, new Timestamp(0L));
            return;
        }
        refreshPositions0(conn, true, -1);
        logger.info("Marked highest visible position - {}", highestVisiblePosition);
        unpublishedPositions.clear();
    }

    private boolean isEnabled() {
        return loaderDatabase.domainDescriptor.isUsesCommitSequencer();
    }

    @RegistryLocation(registryPoint = ClusteredSequencing.class, implementationType = ImplementationType.INSTANCE)
    @Registration(ClusteredSequencing.class)
    public static class ClusteredSequencing {

        public boolean isPrimarySequenceRefresher() {
            return true;
        }
    }
}

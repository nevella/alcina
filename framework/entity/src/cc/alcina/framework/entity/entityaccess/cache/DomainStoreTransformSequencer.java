package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.projection.GraphProjection;

/**
 * dtrp - start persist time (set in persister), committime
 * 
 * and that's all she wrote. updater sets persist/committime to update time,
 * 
 * TODO - this really bridges the db side (and in the right package for that)
 * and domain transform queues - would be nice to split in two and reduce
 * visibility of dtrq package methods. And re-implement as subclass
 * (commit/dbcommit) sequencing
 * 
 * @author nick@alcina.cc
 *
 */
public class DomainStoreTransformSequencer {
	private DomainStoreLoaderDatabase loaderDatabase;

	Logger logger = LoggerFactory.getLogger(getClass());

	private HighestVisibleTransactions highestVisibleTransactions;

	private Connection connection;

	// all access via synchronized methods
	Map<Long, CountDownLatch> preLocalNonFireEventsThreadBarrier = new LinkedHashMap<>();

	Map<Long, CountDownLatch> postLocalFireEventsThreadBarrier = new LinkedHashMap<>();

	DomainStoreTransformSequencer(DomainStoreLoaderDatabase loaderDatabase) {
		this.loaderDatabase = loaderDatabase;
	}

	public synchronized void finishedFiringLocalEvent(long requestId) {
		if (!loaderDatabase.domainDescriptor
				.isUseTransformDbCommitSequencing()) {
			return;
		}
		preLocalNonFireEventsThreadBarrier.remove(requestId);
		logger.trace("Removing post-local barrier: {}", requestId);
		CountDownLatch removed = postLocalFireEventsThreadBarrier
				.remove(requestId);
		removed.countDown();
	}

	public synchronized List<Long> getSequentialUnpublishedTransformIds() {
		try {
			return getSequentialUnpublishedTransformIds0();
		} catch (Exception e) {
			// FIXME - log. Wait for retry (which will be pushed from kafka)
			e.printStackTrace();
			connection = null;
			return new ArrayList<>();
		}
	}

	public synchronized void
			removePreLocalNonFireEventsThreadBarrier(long requestId) {
		logger.trace("Remove local barrier: {}", requestId);
		CountDownLatch latch = preLocalNonFireEventsThreadBarrier
				.get(requestId);
		latch.countDown();
	}

	// called by the main firing sequence thread, since the local vm transforms
	// are fired on the transforming thread
	public void waitForPostLocalFireEventsThreadBarrier(long requestId) {
		try {
			CountDownLatch barrier = null;
			synchronized (this) {
				barrier = postLocalFireEventsThreadBarrier.get(requestId);
			}
			if (barrier == null) {
				logger.warn("Already past barrier (that was quick...) {}",
						requestId);
				return;
			}
			// wait longer - local transforms are more important to fire in
			// order. if this is blocking, that be a prob...but why?
			logger.trace("Wait for post-local barrier: {}", requestId);
			boolean normalExit = barrier.await(20, TimeUnit.SECONDS);
			if (!normalExit) {
				Thread blockingThread = loaderDatabase.getStore()
						.getPersistenceEvents().getQueue().getFiringThread();
				String blockingThreadStacktrace = blockingThread == null
						? "(No firing thread)"
						: SEUtilities.getFullStacktrace(blockingThread);
				logger.warn(
						"Timedout waiting for local vm transform - {} - \n{}\nBlocking thread:\n{}",
						requestId, debugString(), blockingThreadStacktrace);
				// FIXME - may need to fire a domainstoreexception here -
				// probable issue with pg/kafka
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// called by the transforming thread, to ensure post-fire events are fired
	// in db order
	public void waitForPreLocalNonFireEventsThreadBarrier(long requestId) {
		if (!loaderDatabase.domainDescriptor
				.isUseTransformDbCommitSequencing()) {
			return;
		} else {
			createPostLocalFireEventsThreadBarrier(requestId);
			CountDownLatch preLocalBarrier = createPreLocalNonFireEventsThreadBarrier(
					requestId);
			loaderDatabase.getStore().getPersistenceEvents().getQueue()
					.sequencedTransformRequestPublished();
			try {
				logger.trace("Wait for pre-local barrier: {}", requestId);
				// don't wait long - this *tries* to apply transforms in order,
				// but we don't want to block local work
				boolean normalExit = preLocalBarrier.await(5, TimeUnit.SECONDS);
				if (!normalExit) {
					Thread blockingThread = loaderDatabase.getStore()
							.getPersistenceEvents().getQueue()
							.getFireEventsThread();
					String blockingThreadStacktrace = SEUtilities
							.getFullStacktrace(blockingThread);
					logger.warn(
							"Timedout waiting for barrier - {} - \n{} - \nBlocking thread:\n{}",
							requestId, debugString(), blockingThreadStacktrace);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private synchronized CountDownLatch
			createPostLocalFireEventsThreadBarrier(long requestId) {
		return postLocalFireEventsThreadBarrier.computeIfAbsent(requestId,
				id -> new CountDownLatch(1));
	}

	private synchronized CountDownLatch
			createPreLocalNonFireEventsThreadBarrier(long requestId) {
		return preLocalNonFireEventsThreadBarrier.computeIfAbsent(requestId,
				id -> new CountDownLatch(1));
	}

	private synchronized String debugString() {
		return Ax.format("Sequencer:\n===========\n%s\n\nQueue:\n=========\n%s",
				GraphProjection.fieldwiseToString(this),
				loaderDatabase.getStore().getPersistenceEvents().getQueue()
						.toDebugString());
	}

	private Connection getConnection() throws SQLException {
		if (connection == null) {
			connection = loaderDatabase.dataSource.getConnection();
			connection.setAutoCommit(false);
		}
		return connection;
	}

	private HighestVisibleTransactions getHighestVisibleTransformRequest(
			Connection conn) throws SQLException {
		try (Statement statement = conn.createStatement()) {
			Class<? extends DomainTransformRequestPersistent> persistentClass = loaderDatabase.domainDescriptor
					.getDomainTransformRequestPersistentClass();
			String tableName = persistentClass.getAnnotation(Table.class)
					.name();
			String sql = Ax.format(
					"select id, transactionCommitTime from %s where transactionCommitTime is not null order by transactionCommitTime desc limit 1",
					tableName);
			HighestVisibleTransactions transactionsData = new HighestVisibleTransactions();
			ResultSet rs = statement.executeQuery(sql);
			transactionsData.commitTimestamp = new Timestamp(0);
			if (rs.next()) {
				transactionsData.commitTimestamp = rs
						.getTimestamp("transactionCommitTime");
				rs.close();
				PreparedStatement idStatement = conn.prepareStatement(Ax.format(
						"select id from %s where transactionCommitTime=? ",
						tableName));
				idStatement.setTimestamp(1, transactionsData.commitTimestamp);
				ResultSet idRs = idStatement.executeQuery();
				while (idRs.next()) {
					transactionsData.transformListIds.add(idRs.getLong("id"));
				}
				logger.trace("Got highestVisible request data : {}",
						transactionsData);
			}
			return transactionsData;
		}
	}

	private List<Long> getSequentialUnpublishedTransformIds0()
			throws Exception {
		List<Long> unpublishedIds = new ArrayList<>();
		Connection conn = getConnection();
		/*
		 * normally, commit times are ensured just after the dtrp is committed
		 * in TransformPersister
		 * 
		 * if the vm crashes between dtrp commit and update, any subsequent
		 * commits will catch that missed transactionCommitTime update
		 * 
		 * so no need to ensure here
		 */
		// ensureTransactionCommitTimes();
		Class<? extends DomainTransformRequestPersistent> persistentClass = loaderDatabase.domainDescriptor
				.getDomainTransformRequestPersistentClass();
		String tableName = persistentClass.getAnnotation(Table.class).name();
		String sql = Ax.format(
				"select id, transactionCommitTime from %s where transactionCommitTime >=? "
						+ "and transactionCommitTime is not null "
						+ "order by transactionCommitTime ",
				tableName);
		try (PreparedStatement pStatement = conn.prepareStatement(sql)) {
			HighestVisibleTransactions transactionsData = new HighestVisibleTransactions();
			Timestamp fromTimestamp = highestVisibleTransactions.commitTimestamp;
			if (CommonUtils.getYear(new Date(fromTimestamp.getTime())) < 1972) {
				fromTimestamp = new Timestamp(fromTimestamp.getTime() + 1);
				logger.trace("Bumping timestamp - {}", fromTimestamp.getTime());
			}
			pStatement.setTimestamp(1, fromTimestamp);
			ResultSet rs = pStatement.executeQuery();
			List<DtrIdTimestamp> txData = new ArrayList<>();
			// normally it'll be one dtr per timestamp. If more than 99999,
			// we're
			// looking at an initial cleanup - ignore
			int maxCurrent = 99999;
			while (rs.next() && maxCurrent-- > 0) {
				DtrIdTimestamp element = new DtrIdTimestamp();
				element.id = rs.getLong("id");
				element.commitTimestamp = rs
						.getTimestamp("transactionCommitTime");
				if (element.commitTimestamp
						.equals(highestVisibleTransactions.commitTimestamp)
						&& highestVisibleTransactions.transformListIds
								.contains(element.id)) {
					continue;// already submitted/published
				}
				txData.add(element);
			}
			if (maxCurrent <= 0) {
				txData.clear();
			}
			txData.stream().map(txd -> txd.id).forEach(unpublishedIds::add);
			Map<Timestamp, List<DtrIdTimestamp>> collect = txData.stream()
					.collect(AlcinaCollectors
							.toKeyMultimap(txd -> txd.commitTimestamp));
			if (collect.isEmpty()) {
			} else {
				Entry<Timestamp, List<DtrIdTimestamp>> last = CommonUtils
						.last(collect.entrySet().iterator());
				List<Long> lastIds = last.getValue().stream().map(txd -> txd.id)
						.collect(Collectors.toList());
				if (last.getKey()
						.equals(highestVisibleTransactions.commitTimestamp)) {
					lastIds.addAll(highestVisibleTransactions.transformListIds);
				}
				highestVisibleTransactions = new HighestVisibleTransactions();
				highestVisibleTransactions.commitTimestamp = last.getKey();
				highestVisibleTransactions.transformListIds = lastIds;
			}
			logger.trace(
					"Added unpublished ids {} - fromTimestamp {} - new timestamp {}",
					unpublishedIds, fromTimestamp,
					highestVisibleTransactions.commitTimestamp);
			return unpublishedIds;
		}
	}

	void ensureTransactionCommitTimes() throws SQLException {
		if (!loaderDatabase.domainDescriptor
				.isUseTransformDbCommitSequencing()) {
			return;
		}
		/* postgres specific */
		Connection conn = getConnection();
		try (Statement statement = conn.createStatement()) {
			Class<? extends DomainTransformRequestPersistent> persistentClass = loaderDatabase.domainDescriptor
					.getDomainTransformRequestPersistentClass();
			String tableName = persistentClass.getAnnotation(Table.class)
					.name();
			String sql = Ax.format(
					"select id,startPersistTime, pg_xact_commit_timestamp(xmin) as commit_timestamp "
							+ "from %s where transactionCommitTime is null order by pg_xact_commit_timestamp(xmin)",
					tableName);
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				long id = rs.getLong("id");
				PreparedStatement updateStatement = conn.prepareStatement(Ax
						.format("update %s set transactionCommitTime=? where id=?",
								tableName));
				Timestamp commit_timestamp = rs
						.getTimestamp("commit_timestamp");
				updateStatement.setTimestamp(1, commit_timestamp);
				updateStatement.setLong(2, id);
				updateStatement.executeUpdate();
				updateStatement.close();
				logger.debug("Updated transactionCommitTime for request {}",
						id);
			}
			conn.commit();
			rs.close();
		}
	}

	void markHighestVisibleTransformList(Connection conn) throws SQLException {
		if (!loaderDatabase.domainDescriptor
				.isUseTransformDbCommitSequencing()) {
			return;
		}
		highestVisibleTransactions = getHighestVisibleTransformRequest(conn);
	}

	static class DtrIdTimestamp {
		long id;

		Timestamp commitTimestamp;

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToString(this);
		}
	}

	static class HighestVisibleTransactions {
		List<Long> transformListIds = new ArrayList<>();

		Timestamp commitTimestamp;

		@Override
		public String toString() {
			return GraphProjection.fieldwiseToString(this);
		}
	}
}

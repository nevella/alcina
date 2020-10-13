package cc.alcina.framework.entity.persistence.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
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
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;

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
public class DomainStoreTransformSequencer {
	private DomainStoreLoaderDatabase loaderDatabase;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private HighestVisibleTransactions highestVisibleTransactions;

	private Connection connection;

	// all access syncrhonized on this map
	Map<Long, CountDownLatch> preLocalNonFireEventsThreadBarrier = new LinkedHashMap<>();

	// all access syncrhonized on this map
	Map<Long, CountDownLatch> postLocalFireEventsThreadBarrier = new LinkedHashMap<>();

	DomainStoreTransformSequencer(DomainStoreLoaderDatabase loaderDatabase) {
		this.loaderDatabase = loaderDatabase;
	}

	public synchronized void finishedFiringLocalEvent(long requestId) {
		if (!loaderDatabase.domainDescriptor
				.isUseTransformDbCommitSequencing()) {
			return;
		}
		synchronized (preLocalNonFireEventsThreadBarrier) {
			preLocalNonFireEventsThreadBarrier.remove(requestId);
		}
		logger.debug("Unblocking post-local barrier: {}", requestId);
		synchronized (postLocalFireEventsThreadBarrier) {
			postLocalFireEventsThreadBarrier.get(requestId).countDown();
		}
	}

	public synchronized List<TransformSequenceEntry>
			getSequentialUnpublishedRequests() {
		try {
			return getSequentialUnpublishedTransformIds0();
		} catch (Exception e) {
			// Wait for retry (which will be pushed from kafka)
			logger.warn(
					"Issue with getSequentialUnpublishedTransformIds - will retry on next queue event",
					e);
			e.printStackTrace();
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			connection = null;
			return new ArrayList<>();
		}
	}

	public void unblockPreLocalNonFireEventsThreadBarrier(long requestId) {
		logger.debug("Unblock local barrier: {}", requestId);
		try {
			CountDownLatch barrier = null;
			long abnormalExitTime = System.currentTimeMillis()
					+ 10 * TimeConstants.ONE_SECOND_MS;
			while (System.currentTimeMillis() < abnormalExitTime) {
				synchronized (preLocalNonFireEventsThreadBarrier) {
					barrier = preLocalNonFireEventsThreadBarrier.get(requestId);
					if (barrier == null) {
						preLocalNonFireEventsThreadBarrier.wait(100);
					} else {
						break;
					}
				}
			}
			if (barrier == null) {
				logger.warn("Timed out waiting for preLocal barrier  {}",
						requestId);
				return;
			} else {
				barrier.countDown();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// called by the main firing sequence thread, since the local vm transforms
	// are fired on the transforming thread
	//
	// there's a chance this will be called earlier than
	// waitForPreLocalNonFireEventsThreadBarrier for the same requestId
	public void waitForPostLocalFireEventsThreadBarrier(long requestId) {
		try {
			CountDownLatch barrier = null;
			long abnormalExitTime = System.currentTimeMillis()
					+ 10 * TimeConstants.ONE_SECOND_MS;
			while (System.currentTimeMillis() < abnormalExitTime) {
				synchronized (postLocalFireEventsThreadBarrier) {
					barrier = postLocalFireEventsThreadBarrier.get(requestId);
					if (barrier == null) {
						postLocalFireEventsThreadBarrier.wait(100);
					} else {
						break;
					}
				}
			}
			if (barrier == null) {
				logger.warn("Timed out waiting for postLocal barrier  {}",
						requestId);
				return;
			}
			// wait longer - local transforms are more important to fire in
			// order. if this is blocking, that be a prob...but why?
			logger.debug("Wait for post-local barrier: {}", requestId);
			boolean normalExit = barrier.await(20, TimeUnit.SECONDS);
			if (!normalExit) {
				Thread blockingThread = loaderDatabase.getStore()
						.getPersistenceEvents().getQueue().getFiringThread();
				String blockingThreadStacktrace = blockingThread == null
						? "(No firing thread)"
						: SEUtilities.getFullStacktrace(blockingThread);
				logger.warn(
						"Timedout waiting for post local barrier/local vm transform - {} - \n{}\nBlocking thread:\n{}",
						requestId, debugString(), blockingThreadStacktrace);
				// FIXME - mvcc.4 - may need to fire a domainstoreexception here
				// -
				// probable issue with pg/kafka. On the other hand, our
				// sequencer logic might be simpler now...(or after
				// mvcc.3/postprocess optimisations)
			} else {
				logger.debug("Wait for post-local barrier complete: {}",
						requestId);
			}
			synchronized (postLocalFireEventsThreadBarrier) {
				postLocalFireEventsThreadBarrier.remove(requestId);
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
					.sequencedTransformRequestPublished(requestId);
			try {
				logger.debug("Wait for pre-local barrier: {}", requestId);
				// this wait is > than the queue restart time -- *really* don't
				// want to commit out of order, but it's better than blocking
				// forever
				int wait = 15;
				boolean normalExit = preLocalBarrier.await(wait,
						TimeUnit.SECONDS);
				if (!normalExit) {
					Thread blockingThread = loaderDatabase.getStore()
							.getPersistenceEvents().getQueue()
							.getFireEventsThread();
					String blockingThreadStacktrace = blockingThread == null
							? "<no blocking thread>"
							: SEUtilities.getFullStacktrace(blockingThread);
					logger.warn(
							"Timedout waiting for barrier - {} - \n{} - \nBlocking thread:\n{}",
							requestId, debugString(), blockingThreadStacktrace);
				} else {
					logger.debug("Wait for pre-local barrier complete: {}",
							requestId);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private CountDownLatch
			createPostLocalFireEventsThreadBarrier(long requestId) {
		logger.debug("Created wait for post-local barrier: {}", requestId);
		synchronized (postLocalFireEventsThreadBarrier) {
			CountDownLatch barrier = postLocalFireEventsThreadBarrier
					.get(requestId);
			if (barrier == null) {
				barrier = new CountDownLatch(1);
				postLocalFireEventsThreadBarrier.put(requestId, barrier);
				postLocalFireEventsThreadBarrier.notifyAll();
			}
			return barrier;
		}
	}

	private CountDownLatch
			createPreLocalNonFireEventsThreadBarrier(long requestId) {
		logger.debug("Created wait for pre-local barrier: {}", requestId);
		synchronized (preLocalNonFireEventsThreadBarrier) {
			CountDownLatch barrier = preLocalNonFireEventsThreadBarrier
					.get(requestId);
			if (barrier == null) {
				barrier = new CountDownLatch(1);
				preLocalNonFireEventsThreadBarrier.put(requestId, barrier);
				preLocalNonFireEventsThreadBarrier.notifyAll();
			}
			return barrier;
		}
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
				try (PreparedStatement idStatement = conn.prepareStatement(Ax
						.format("select id from %s where transactionCommitTime=? ",
								tableName))) {
					idStatement.setTimestamp(1,
							transactionsData.commitTimestamp);
					ResultSet idRs = idStatement.executeQuery();
					while (idRs.next()) {
						transactionsData.transformListIds
								.add(idRs.getLong("id"));
					}
					logger.debug("Got highestVisible request data : {}",
							transactionsData);
				}
			}
			conn.commit();
			return transactionsData;
		}
	}

	private List<TransformSequenceEntry> getSequentialUnpublishedTransformIds0()
			throws Exception {
		if (highestVisibleTransactions == null) {
			// not yet finished with marking - come back later please
			return Collections.emptyList();
		}
		List<TransformSequenceEntry> unpublishedIds = new ArrayList<>();
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
				logger.debug("Bumping timestamp - {}", fromTimestamp.getTime());
			}
			pStatement.setTimestamp(1, fromTimestamp);
			ResultSet rs = pStatement.executeQuery();
			List<TransformSequenceEntry> txData = new ArrayList<>();
			// normally it'll be one dtr per timestamp. If more than 99999,
			// we're
			// looking at an initial cleanup - ignore
			int maxCurrent = 99999;
			while (rs.next() && maxCurrent-- > 0) {
				TransformSequenceEntry entry = new TransformSequenceEntry();
				entry.persistentRequestId = rs.getLong("id");
				entry.commitTimestamp = rs
						.getTimestamp("transactionCommitTime");
				if (entry.commitTimestamp
						.equals(highestVisibleTransactions.commitTimestamp)
						&& highestVisibleTransactions.transformListIds
								.contains(entry.persistentRequestId)) {
					continue;// already submitted/published
				}
				txData.add(entry);
			}
			rs.close();
			if (maxCurrent <= 0) {
				txData.clear();
			}
			txData.forEach(unpublishedIds::add);
			Map<Timestamp, List<TransformSequenceEntry>> collect = txData
					.stream().collect(AlcinaCollectors
							.toKeyMultimap(txd -> txd.commitTimestamp));
			if (collect.isEmpty()) {
			} else {
				Entry<Timestamp, List<TransformSequenceEntry>> last = CommonUtils
						.last(collect.entrySet().iterator());
				List<Long> lastIds = last.getValue().stream()
						.map(txd -> txd.persistentRequestId)
						.collect(Collectors.toList());
				if (last.getKey()
						.equals(highestVisibleTransactions.commitTimestamp)) {
					lastIds.addAll(highestVisibleTransactions.transformListIds);
				}
				highestVisibleTransactions = new HighestVisibleTransactions();
				highestVisibleTransactions.commitTimestamp = last.getKey();
				highestVisibleTransactions.transformListIds = lastIds;
			}
			logger.debug(
					"Added unpublished ids {} - fromTimestamp {} - new timestamp {}",
					unpublishedIds, fromTimestamp,
					highestVisibleTransactions.commitTimestamp);
		}
		conn.commit();
		return unpublishedIds;
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
				try (PreparedStatement updateStatement = conn
						.prepareStatement(Ax.format(
								"update %s set transactionCommitTime=? where id=?",
								tableName))) {
					long id = rs.getLong("id");
					Timestamp commit_timestamp = rs
							.getTimestamp("commit_timestamp");
					updateStatement.setTimestamp(1, commit_timestamp);
					updateStatement.setLong(2, id);
					updateStatement.executeUpdate();
					updateStatement.close();
					logger.debug("Updated transactionCommitTime for request {}",
							id);
				}
			}
		}
		conn.commit();
	}

	Timestamp getHighestVisibleTransactionTimestamp() {
		return highestVisibleTransactions.commitTimestamp;
	}

	void markHighestVisibleTransformList(Connection conn) throws SQLException {
		if (!loaderDatabase.domainDescriptor
				.isUseTransformDbCommitSequencing()) {
			return;
		}
		highestVisibleTransactions = getHighestVisibleTransformRequest(conn);
		if (highestVisibleTransactions == null) {
			throw new RuntimeException("Null h.v.t");
		} else {
			logger.info("Marked highest visible transactions - {}",
					highestVisibleTransactions);
		}
	}

	public static class TransformSequenceEntry {
		public long persistentRequestId;

		public Timestamp commitTimestamp;

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

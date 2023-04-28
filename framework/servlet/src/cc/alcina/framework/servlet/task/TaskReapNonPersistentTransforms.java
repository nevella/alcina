package cc.alcina.framework.servlet.task;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Table;

import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LongPair;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.logic.PersistentAppProperties;
import cc.alcina.framework.entity.persistence.domain.DataSourceAdapter;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.projection.EntityPersistenceHelper;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.policy.TransformPropagationPolicy;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.entity.util.SqlUtils;
import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.job.JobContext;

public class TaskReapNonPersistentTransforms extends PerformerTask {
	private static final transient int SLICE_SIZE = 1000;

	public static final transient String TRANSFORM_REAPER_2_LAST_RQ_ID = "TRANSFORM_REAPER_2_LAST_RQ_ID";

	private transient Statement stmt;

	private ResultSet executeQuery(String template, Object... args)
			throws Exception {
		return stmt.executeQuery(Ax.format(template, args));
	}

	protected void reap() throws Exception {
		Connection conn = Registry.impl(DataSourceAdapter.class)
				.getConnection();
		try {
			long lastId = PersistentAppProperties
					.getLong(TRANSFORM_REAPER_2_LAST_RQ_ID);
			stmt = conn.createStatement();
			String dtrTableName = PersistentImpl
					.getImplementation(DomainTransformRequestPersistent.class)
					.getAnnotation(Table.class).name();
			String dteTableName = PersistentImpl
					.getImplementation(DomainTransformEventPersistent.class)
					.getAnnotation(Table.class).name();
			ResultSet rs = executeQuery("select max(id) from %s", dtrTableName);
			rs.next();
			long maxId = rs.getLong(1);
			logger.info("Requests to check: {}", new LongPair(lastId, maxId));
			while (true) {
				String rSql = String.format(
						"select dtrq.id from %s "
								+ "dtrq where id>=%s and id<%s order by id",
						dtrTableName, lastId, lastId + SLICE_SIZE);
				Set<Long> rqIds = SqlUtils.toIdSet(stmt, rSql, "id", false);
				for (Long id : rqIds) {
					DomainTransformRequestPersistent request = DomainStore
							.writableStore().loadTransformRequest(id);
					if (request == null) {
						// don't reap (anymore) - useful metadata
						continue;
					}
					if (System.currentTimeMillis()
							- request.getTransactionCommitTime()
									.getTime() < TimeConstants.ONE_DAY_MS) {
						logger.info("At now() - 1 days, exiting");
						return;
					}
					TransformPropagationPolicy policy = Registry
							.impl(TransformPropagationPolicy.class);
					List<Long> toDeleteTransformIds = request.getEvents()
							.stream().filter(event -> {
								if (event.getObjectClass() == null) {
									return true;
								}
								return !policy.shouldPersistEventRecord(event);
							}).map(e -> ((DomainTransformEventPersistent) e)
									.getId())
							.collect(Collectors.toList());
					if (toDeleteTransformIds.size() > 0) {
						logger.info(
								"dtr: {} :: initial transforms :: {} :: will delete :: {}",
								id, request.getEvents().size(),
								toDeleteTransformIds.size());
						stmt.execute(Ax.format("delete from %s where id in %s",
								dteTableName, EntityPersistenceHelper
										.toInClause(toDeleteTransformIds)));
						logger.info("deleted :: {}",
								toDeleteTransformIds.size());
					}
				}
				lastId += SLICE_SIZE;
				Transaction.endAndBeginNew();
				PersistentAppProperties.setLong(TRANSFORM_REAPER_2_LAST_RQ_ID,
						lastId);
				Transaction.commit();
				Transaction.endAndBeginNew();
				JobContext.checkCancelled();
				if (lastId > maxId) {
					System.out.println("\n=====Hit transform - ending\n\n");
					break;
				}
			}
		} finally {
			conn.close();
		}
	}

	@Override
	public void run() throws Exception  {
		// run as root, otherwise shouldPersistEventRecord will always return
		// true
		MethodContext.instance().withRootPermissions(true).run(this::reap);
	}
}
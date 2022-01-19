package cc.alcina.framework.servlet.task;

import java.sql.Connection;
import java.sql.Statement;

import javax.persistence.Table;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.CommonPersistenceBase.CommonPersistenceConnectionProvider;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.util.SqlUtils;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

public class TaskFixHibernateDatetimePersistentType
		extends AbstractTaskPerformer {
	@Override
	protected void run0() throws Exception {
		try (Connection conn = Registry
				.impl(CommonPersistenceConnectionProvider.class)
				.getConnection()) {
			Statement stmt = conn.createStatement();
			String tableName = PersistentImpl
					.getImplementation(DomainTransformRequestPersistent.class)
					.getAnnotation(Table.class).name();
			try {
				SqlUtils.execute(stmt, Ax.format(
						"alter table %s alter column startpersisttime type timestamptz",
						tableName));
				SqlUtils.execute(stmt, Ax.format(
						"alter table %s alter column transactioncommittime type timestamptz",
						tableName));
			} catch (Exception e) {
				System.out.println("Statement executed");
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}

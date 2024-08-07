package cc.alcina.framework.entity.persistence.updater;

import java.sql.Connection;
import java.sql.Statement;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.persistence.CommonPersistenceBase.CommonPersistenceConnectionProvider;

public abstract class DbUpdater implements Comparable<DbUpdater> {
	public boolean allowNullEntityManager() {
		return false;
	}

	@Override
	public int compareTo(DbUpdater o) {
		return getUpdateNumber().compareTo(o.getUpdateNumber());
	}

	protected void ex(Statement stmt, String sql) throws Exception {
		ex(stmt, sql, false);
	}

	protected void ex(Statement stmt, String sql, boolean squelchErrors)
			throws Exception {
		MetricLogging.get().start("query");
		System.out.println(sql);
		if (squelchErrors) {
			try {
				stmt.execute(sql);
			} catch (Exception e) {
				if (e.getMessage().contains("already exists")) {
					System.out.println("---ignore - already exists");
				} else {
					e.printStackTrace();
				}
			}
		} else {
			stmt.execute(sql);
		}
		MetricLogging.get().end("query");
	}

	protected void exSql(String sql) throws Exception {
		exSql(sql, false);
	}

	protected void exSql(String sql, boolean squelchErrors) throws Exception {
		System.out.println("SQL: " + sql);
		int j;
		Statement stmt;
		try (Connection conn = Registry
				.impl(CommonPersistenceConnectionProvider.class)
				.getConnection()) {
			stmt = conn.createStatement();
			try {
				j = stmt.executeUpdate(sql);
				System.out.println(j + "  results");
			} catch (Exception e) {
				stmt.execute(sql);
				System.out.println("Statement executed");
			}
		} catch (Exception e) {
			if (e.getMessage().contains("already exists")) {
				System.out.println("---ignore - already exists");
			} else {
				if (!squelchErrors) {
					throw e;
				}
				System.out.println(e.getMessage());
			}
		}
	}

	public abstract Integer getUpdateNumber();

	public abstract void run(EntityManager em) throws Exception;

	public boolean runAsync() {
		return false;
	}

	public boolean runPreCache() {
		return false;
	}
}

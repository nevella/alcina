package cc.alcina.framework.entity.entityaccess.updaters;

import java.sql.Connection;
import java.sql.Statement;

import javax.persistence.EntityManager;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceBase.CommonPersistenceConnectionProvider;

public abstract class DbUpdater implements Comparable<DbUpdater> {
    @Override
    public int compareTo(DbUpdater o) {
        return getUpdateNumber().compareTo(o.getUpdateNumber());
    }

    public boolean runAsync() {
        return false;
    }

    public boolean runPreCache() {
        return false;
    }

    public abstract Integer getUpdateNumber();

    public abstract void run(EntityManager em) throws Exception;

    protected void exSql(String sql) throws Exception {
        exSql(sql, false);
    }

    protected void exSql(String sql, boolean squelchErrors) throws Exception {
        System.out.println("SQL: " + sql);
        int j;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = Registry.impl(CommonPersistenceConnectionProvider.class)
                    .getConnection();
            stmt = conn.createStatement();
            j = stmt.executeUpdate(sql);
            System.out.println(j + "  results");
        } catch (Exception e) {
            if (e.getMessage().contains("already exists")) {
                System.out.println("---ignore - already exists");
            } else {
                if (!squelchErrors) {
                    throw e;
                }
                System.out.println(e.getMessage());
            }
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                conn.close();
            } catch (Exception e) {
                throw new WrappedRuntimeException(e);
            }
        }
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

    public boolean allowNullEntityManager() {
        return false;
    }
}

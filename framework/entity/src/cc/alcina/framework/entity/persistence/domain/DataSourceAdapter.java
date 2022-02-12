package cc.alcina.framework.entity.persistence.domain;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Supplier;

import javax.sql.DataSource;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = DataSourceAdapter.class, implementationType = ImplementationType.INSTANCE)
@Registration(DataSourceAdapter.class)
public abstract class DataSourceAdapter implements DataSource {
	private Supplier<Connection> supplier;

	public DataSourceAdapter() {
	}

	public DataSourceAdapter(Supplier<Connection> supplier) {
		this.supplier = supplier;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return supplier.get();
	}

	@Override
	public Connection getConnection(String username, String password)
			throws SQLException {
		return supplier.get();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return null;
	}

	@Override
	public java.util.logging.Logger getParentLogger()
			throws SQLFeatureNotSupportedException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}
}

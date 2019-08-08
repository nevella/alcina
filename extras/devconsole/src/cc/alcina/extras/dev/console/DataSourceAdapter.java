package cc.alcina.extras.dev.console;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Supplier;

import javax.sql.DataSource;

public class DataSourceAdapter implements DataSource {
	private Supplier<Connection> supplier;

	public DataSourceAdapter(Supplier<Connection> supplier) {
		this.supplier = supplier;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return null;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	@Override
	public java.util.logging.Logger getParentLogger()
			throws SQLFeatureNotSupportedException {
		return null;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return supplier.get();
	}

	@Override
	public Connection getConnection(String username, String password)
			throws SQLException {
		return null;
	}
}
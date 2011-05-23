package cc.alcina.framework.jvmclient.persistence;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.ResourceUtilities;

public class EmbeddedDerbyTransformPersistence extends JdbcTransformPersistence {
	public EmbeddedDerbyTransformPersistence(String derbyHomePath) {
		this(derbyHomePath, null, "org.apache.derby.jdbc.EmbeddedDriver");
	}

	public EmbeddedDerbyTransformPersistence(String derbyHomePath,
			Class driverClass, String driverClassName) {
		super();
		System.setProperty("derby.system.home", derbyHomePath);
		if (driverClass == null) {
			try {
				Class.forName(driverClassName);
			} catch (ClassNotFoundException e) {
				throw new WrappedRuntimeException(e);
			}
		}
		String dbName = "persistedtransforms";
		String connectionUrl = "jdbc:derby:" + dbName + ";create=true";
		setConnectionUrl(connectionUrl);
		if (!checkDbVersionOK()) {
			String createSmt = null;
			try {
				InputStream is = this.getClass().getResourceAsStream(
						"derby-create.sql");
				Connection conn = DriverManager
						.getConnection(getConnectionUrl());
				String createStmt = ResourceUtilities.readStreamToString(is);
				Statement s = conn.createStatement();
				s.execute(createStmt);
				s.close();
				conn.close();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	private boolean checkDbVersionOK() {
		try {
			Connection conn = DriverManager.getConnection(getConnectionUrl());
			Statement s = conn.createStatement();
			s.execute("select * from TransformRequests");
			s.close();
			conn.close();
			return true;
		} catch (SQLException sqle) {
			String theError = (sqle).getSQLState();
			if (theError.equals("42X05")) // Table does not exist
			{
				return false;
			}
			sqle.printStackTrace();
			throw new RuntimeException(sqle);
		}
	}

	@Override
	public String getPersistenceStoreName() {
		return "Apache Derby";
	}
}

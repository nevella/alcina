package cc.alcina.framework.jvmclient.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class EmbeddedDerbyTransformPersistence
		extends JdbcTransformPersistence {
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
		setLocalStorageInstalled(true);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					DriverManager.getConnection("jdbc:derby:;shutdown=true");
				} catch (SQLException e) {
					// e.printStackTrace();
					// will always throw an exception
				}
			}
		});
		try {
			if (!checkDbVersionOK()) {
				Connection conn = DriverManager
						.getConnection(getConnectionUrl());
				String createStmt = getCreateStatement();
				Statement s = conn.createStatement();
				s.execute(createStmt);
				s.close();
				conn.close();
			}
		} catch (Exception e) {
			setLocalStorageInstalled(false);
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public String getPersistenceStoreName() {
		return "Apache Derby";
	}

	private boolean checkDbVersionOK() {
		try (Connection conn = DriverManager
				.getConnection(getConnectionUrl())) {
			Statement s = conn.createStatement();
			try {
				s.execute("select * from TransformRequests");
			} catch (SQLException sqle) {
				String theError = (sqle).getSQLState();
				if (theError.equals("42X05")) // Table does not exist
				{
					return false;
				}
				sqle.printStackTrace();
				throw new RuntimeException(sqle);
			}
			try {
				s.execute(
						"select chunk_uuid from TransformRequests where id=-1");
			} catch (SQLException sqle) {
				s.executeUpdate(
						"alter table TransformRequests add column chunk_uuid varchar(200)");
			}
			return true;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private String getCreateStatement() {
		return "CREATE TABLE TransformRequests (\n"
				+ "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1,\n"
				+ " INCREMENT BY 1) ,\n" + " transform CLOB,\n"
				+ " timestamp BIGINT,\n" + " user_id BIGINT,\n"
				+ " clientInstance_id BIGINT,\n" + " request_id BIGINT,\n"
				+ " clientInstance_auth INTEGER,\n"
				+ "   transform_request_type varchar(50),\n"
				+ "  transform_event_protocol varchar(50),\n"
				+ "  tag varchar(50) ,\nchunk_uuid varchar(200) ,\n"
				+ "  PRIMARY KEY (id)\n" + ") \n";
	}
}

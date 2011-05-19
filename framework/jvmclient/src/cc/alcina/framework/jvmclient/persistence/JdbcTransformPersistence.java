/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.jvmclient.persistence;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.persistence.client.DTESerializationPolicy;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;
import cc.alcina.framework.gwt.persistence.client.PersistenceCallback;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public abstract class JdbcTransformPersistence extends
		LocalTransformPersistence {
	private String connectionUrl;

	public String getConnectionUrl() {
		return this.connectionUrl;
	}

	public void setConnectionUrl(String connectionUrl) {
		this.connectionUrl = connectionUrl;
	}

	public JdbcTransformPersistence() {
	}

	@Override
	public void clearPersistedClient(final ClientInstance exceptFor,
			final PersistenceCallback callback) {
		try {
			executeStatement("DELETE from TransformRequests"
					+ " where (transform_request_type='CLIENT_OBJECT_LOAD'"
					+ " OR transform_request_type='CLIENT_SYNC'"
					+ " OR transform_request_type='TO_REMOTE_COMPLETED')"
					+ " and clientInstance_id != "
					+ (exceptFor == null ? -1 : exceptFor.getId()));
			callback.onSuccess(null);
		} catch (Exception e) {
			callback.onFailure(e);
		}
	}

	@Override
	protected void getTransforms(final DomainTransformRequestType[] types,
			final PersistenceCallback<List<DTRSimpleSerialWrapper>> callback) {
		Object[] params = { "id", Integer.class, "transform", String.class,
				"timestamp", Long.class, "user_id", Long.class,
				"clientInstance_id", Long.class, "request_id", Integer.class,
				"clientInstance_auth", Integer.class, "transform_request_type",
				DomainTransformRequestType.class, "transform_event_protocol",
				String.class, "tag", String.class };
		List<DTRSimpleSerialWrapper> transforms = new ArrayList<DTRSimpleSerialWrapper>();
		String sql = "select * from TransformRequests ";
		for (int i = 0; i < types.length; i++) {
			sql += i == 0 ? " where (" : " or ";
			sql += CommonUtils.format("transform_request_type='%1'", types[i]);
		}
		sql += ") ";
		if (getClientInstanceIdForGet() != null) {
			sql += CommonUtils.format(" and clientInstance_id=%1 ",
					getClientInstanceIdForGet());
		}
		sql += "  order by id asc";
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Map<String, Object> map = getFieldsAs(rs, params);
				DTRSimpleSerialWrapper wr = new DTRSimpleSerialWrapper(
						(Integer) map.get("id"), (String) map.get("transform"),
						(Long) map.get("timestamp"), (Long) map.get("user_id"),
						(Long) map.get("clientInstance_id"),
						(Integer) map.get("request_id"),
						(Integer) map.get("clientInstance_auth"),
						(DomainTransformRequestType) map
								.get("transform_request_type"),
						(String) map.get("transform_event_protocol"),
						(String) map.get("tag"));
				transforms.add(wr);
			}
		} catch (Exception e) {
			callback.onFailure(e);
		} finally {
			cleanup(conn, stmt, rs);
		}
		callback.onSuccess(transforms);
	}

	@Override
	public void init(
			final DTESerializationPolicy dteSerializationPolicy,
			final CommitToStorageTransformListener commitToServerTransformListener,
			final PersistenceCallback callback) {
		final LocalTransformPersistence listener = this;
		final PersistenceCallback superCallback = new PersistenceCallback() {
			@Override
			public void onSuccess(Object result) {
				getCommitToStorageTransformListener().addStateChangeListener(
						listener);
				ClientTransformManager.cast().setPersistableTransformListener(
						listener);
				callback.onSuccess(null);
			}

			@Override
			public void onFailure(Throwable caught) {
				setLocalStorageInstalled(false);
				callback.onFailure(caught);
			}
		};
		ensureDb(new PersistenceCallback() {
			@Override
			public void onFailure(Throwable caught) {
				setLocalStorageInstalled(false);
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(Object result) {
				initSuper(dteSerializationPolicy,
						commitToServerTransformListener, superCallback);
			}
		});
		callback.onSuccess(null);
	}

	private void initSuper(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener,
			final PersistenceCallback superCallback) {
		super.init(dteSerializationPolicy, commitToServerTransformListener,
				superCallback);
	}

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(connectionUrl);
	}

	private void executeStatement(String sql) {
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			stmt.execute(sql);
		} catch (Exception e) {
			throw new WrappedRuntimeException("Problem accessing local db", e,
					SuggestedAction.NOTIFY_WARNING);
		} finally {
			cleanup(conn, stmt, null);
		}
	}

	private void cleanup(Connection conn, Statement stmt, ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				conn.close();
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException("Problem accessing local db", e,
					SuggestedAction.NOTIFY_WARNING);
		}
	}

	protected void ensureDb(final PersistenceCallback callback) {
		// should be done elsewhere (for generic jdbc)(generally done in
		// constructor, so this is a noop)
		callback.onSuccess(null);
	}

	private Map<String, Object> getFieldsAs(ResultSet rs, Object[] params)
			throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		for (int i = 0; i < params.length; i += 2) {
			String paramName = (String) params[i];
			Class paramClass = (Class) params[i + 1];
			Object value = null;
			if (paramClass == Long.class) {
				value = rs.getLong(paramName);
			}
			if (paramClass == Integer.class) {
				value = rs.getInt(paramName);
			}
			if (paramClass == String.class) {
				value = rs.getString(paramName);
			}
			if (paramClass.isEnum()) {
				value = Enum.valueOf(paramClass, rs.getString(paramName));
			}
			result.put(paramName, value);
		}
		return result;
	}

	@Override
	protected void clearAllPersisted(final PersistenceCallback callback) {
		try {
			executeStatement("DELETE from TransformRequests");
			callback.onSuccess(null);
		} catch (Exception e) {
			callback.onFailure(e);
		}
	}

	@Override
	protected void persist(final DTRSimpleSerialWrapper wrapper,
			final PersistenceCallback callback) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			pstmt = conn
					.prepareStatement("INSERT INTO TransformRequests "
							+ "(transform, timestamp,"
							+ "user_id,clientInstance_id"
							+ ",request_id,clientInstance_auth,"
							+ "transform_request_type,transform_event_protocol,tag) VALUES (?, ?,?,?,?,?,?,?,?)");
			if (wrapper.getProtocolVersion() == null) {
				throw new Exception("wrapper must have protocol version");
			}
			Clob clob = conn.createClob();
			clob.setString(1, wrapper.getText());
			pstmt.setClob(1, clob);
			pstmt.setLong(2, wrapper.getTimestamp());
			pstmt.setLong(3, wrapper.getUserId());
			pstmt.setLong(4, wrapper.getClientInstanceId());
			pstmt.setLong(5, wrapper.getRequestId());
			pstmt.setLong(6, wrapper.getClientInstanceAuth());
			pstmt.setString(7, wrapper.getDomainTransformRequestType()
					.toString());
			pstmt.setString(8, wrapper.getProtocolVersion());
			pstmt.setString(9, wrapper.getTag());
			pstmt.execute();
			rs = pstmt.getGeneratedKeys();
			if (rs != null && rs.next()) {
				int newid = rs.getInt(1);
				wrapper.setId(newid);
			}
			if (wrapper.getDomainTransformRequestType() == DomainTransformRequestType.CLIENT_OBJECT_LOAD) {
				clearPersistedClient(ClientLayerLocator.get()
						.getClientInstance(), callback);
			} else {
				callback.onSuccess(null);
			}
			return;
		} catch (Exception e) {
			callback.onFailure(e);
		} finally {
			cleanup(conn, pstmt, rs);
		}
	}

	@Override
	protected void transformPersisted(
			final List<DTRSimpleSerialWrapper> persistedWrappers,
			final PersistenceCallback callback) {
		try {
			for (DTRSimpleSerialWrapper wrapper : persistedWrappers) {
				executeStatement("update  TransformRequests  set "
						+ "transform_request_type='TO_REMOTE_COMPLETED'"
						+ " where id = " + wrapper.getId());
			}
			callback.onSuccess(null);
		} catch (Exception e) {
			callback.onFailure(e);
		}
	}

	@Override
	protected void reparentToClientInstance(
			final DTRSimpleSerialWrapper wrapper,
			final ClientInstance clientInstance,
			final PersistenceCallback callback) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement("update  TransformRequests  set "
					+ "clientInstance_id=?,clientInstance_auth=? "
					+ " where id = ?");
			pstmt.setLong(1, clientInstance.getId());
			pstmt.setInt(2, clientInstance.getAuth());
			pstmt.setInt(3, wrapper.getId());
			callback.onSuccess(null);
		} catch (SQLException e) {
			callback.onFailure(e);
		} finally {
			cleanup(conn, pstmt, rs);
		}
	}
}

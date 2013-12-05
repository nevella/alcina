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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DomainTrancheProtocolHandler;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.renderer.ToStringConverter;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.persistence.client.DTESerializationPolicy;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;
import cc.alcina.framework.gwt.persistence.client.DtrWrapperBackedDomainModelDelta.DeltaApplicationRecordToDomainModelDeltaConverter;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence.DeltaApplicationFilters;

import com.google.gwt.user.client.rpc.AsyncCallback;

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
			int exceptForId, final AsyncCallback callback,
			boolean clearDeltaStore) {
		try {
			String sql = CommonUtils.formatJ("DELETE from TransformRequests"
					+ " where (transform_request_type='CLIENT_OBJECT_LOAD'"
					+ " OR transform_request_type='CLIENT_SYNC'"
					+ " OR transform_request_type='TO_REMOTE_COMPLETED')"
					+ " and (clientInstance_id != %s and id != %s)",
					exceptFor == null ? -1 : exceptFor.getId(), exceptForId);
			executeStatement(sql);
			callback.onSuccess(null);
		} catch (Exception e) {
			callback.onFailure(e);
		}
	}

	Object[] transformParams = { "id", Integer.class, "transform",
			String.class, "timestamp", Long.class, "user_id", Long.class,
			"clientInstance_id", Long.class, "request_id", Integer.class,
			"clientInstance_auth", Integer.class, "transform_request_type",
			DeltaApplicationRecordType.class, "transform_event_protocol",
			String.class, "tag", String.class };

	protected ResultSet getTransformsResultSet(
			final DeltaApplicationFilters filters, CleanupTuple tuple)
			throws SQLException {
		String sql = getTransformWrapperSql(filters);
		return tuple.executeQuery(sql);
	}

	class CleanupTuple {
		Connection conn = null;

		Statement stmt = null;

		PreparedStatement pstmt = null;

		ResultSet rs = null;

		public CleanupTuple() {
			try {
				conn = getConnection();
				stmt = conn.createStatement();
			} catch (Exception e) {
				cleanup();
				throw new WrappedRuntimeException(e);
			}
		}

		ResultSet executeQuery(String sql) throws SQLException {
			try {
				return stmt.executeQuery(sql);
			} catch (SQLException e) {
				cleanup();
				throw e;
			}
		}

		void execute(String sql) throws SQLException {
			try {
				stmt.execute(sql);
			} catch (SQLException e) {
				cleanup();
				throw e;
			}
		}

		void cleanup() {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException("Problem accessing local db",
						e, SuggestedAction.NOTIFY_WARNING);
			}
		}

		public PreparedStatement prepareStatement(String sql)
				throws SQLException {
			pstmt = conn.prepareStatement(sql);
			return pstmt;
		}

		public void executePstmt() throws SQLException {
			pstmt.execute();
		}

		public ResultSet getGeneratedKeys() throws SQLException {
			return pstmt.getGeneratedKeys();
		}
	}

	@Override
	public void init(
			final DTESerializationPolicy dteSerializationPolicy,
			final CommitToStorageTransformListener commitToServerTransformListener,
			final AsyncCallback callback) {
		final LocalTransformPersistence listener = this;
		final AsyncCallback superCallback = new AsyncCallback() {
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
		ensureDb(new AsyncCallback() {
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
			final AsyncCallback superCallback) {
		super.init(dteSerializationPolicy, commitToServerTransformListener,
				superCallback);
	}

	public Connection getConnection() {
		try {
			return DriverManager.getConnection(connectionUrl);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void executeStatement(String sql) {
		CleanupTuple cleanupTuple = new CleanupTuple();
		try {
			cleanupTuple.execute(sql);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			cleanupTuple.cleanup();
		}
	}

	protected void ensureDb(final AsyncCallback callback) {
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
	protected void clearAllPersisted(final AsyncCallback callback) {
		try {
			executeStatement("DELETE from TransformRequests");
			callback.onSuccess(null);
		} catch (Exception e) {
			callback.onFailure(e);
		}
	}

	@Override
	protected void persistFromFrontOfQueue(
			final DeltaApplicationRecord wrapper, final AsyncCallback callback) {
		CleanupTuple tuple = new CleanupTuple();
		try {
			PreparedStatement pstmt = tuple
					.prepareStatement("INSERT INTO TransformRequests "
							+ "(transform, timestamp,"
							+ "user_id,clientInstance_id"
							+ ",request_id,clientInstance_auth,"
							+ "transform_request_type,transform_event_protocol,tag) VALUES (?, ?,?,?,?,?,?,?,?)");
			if (wrapper.getProtocolVersion() == null) {
				throw new Exception("wrapper must have protocol version");
			}
			Clob clob = tuple.conn.createClob();
			clob.setString(1, wrapper.getText());
			pstmt.setClob(1, clob);
			pstmt.setLong(2, wrapper.getTimestamp());
			pstmt.setLong(3, wrapper.getUserId());
			pstmt.setLong(4, wrapper.getClientInstanceId());
			pstmt.setLong(5, wrapper.getRequestId());
			pstmt.setLong(6, wrapper.getClientInstanceAuth());
			pstmt.setString(7, wrapper.getType().toString());
			pstmt.setString(8, wrapper.getProtocolVersion());
			pstmt.setString(9, wrapper.getTag());
			tuple.executePstmt();
			ResultSet rs = tuple.getGeneratedKeys();
			if (rs != null && rs.next()) {
				int newid = rs.getInt(1);
				wrapper.setId(newid);
			}
			callback.onSuccess(null);
		} catch (Exception e) {
			callback.onFailure(e);
		} finally {
			tuple.cleanup();
		}
	}

	@Override
	protected void transformPersisted(
			final List<DeltaApplicationRecord> persistedWrappers,
			final AsyncCallback callback) {
		try {
			for (DeltaApplicationRecord wrapper : persistedWrappers) {
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
	public void reparentToClientInstance(final DeltaApplicationRecord wrapper,
			final ClientInstance clientInstance, final AsyncCallback callback) {
		CleanupTuple tuple = new CleanupTuple();
		try {
			PreparedStatement pstmt = tuple
					.prepareStatement("update  TransformRequests  set "
							+ "clientInstance_id=?,clientInstance_auth=? "
							+ " where id = ?");
			pstmt.setLong(1, clientInstance.getId());
			pstmt.setInt(2, clientInstance.getAuth());
			pstmt.setInt(3, wrapper.getId());
			callback.onSuccess(null);
		} catch (SQLException e) {
			callback.onFailure(e);
		} finally {
			tuple.cleanup();
		}
	}

	public void reparentToClientInstance(long clientInstanceId,
			ClientInstance clientInstance, AsyncCallback callback) {
		CleanupTuple tuple = new CleanupTuple();
		try {
			PreparedStatement pstmt = tuple
					.prepareStatement("update TransformRequests set "
							+ "CLIENTINSTANCE_ID=?,CLIENTINSTANCE_AUTH=? "
							+ "where CLIENTINSTANCE_ID = ?");
			pstmt.setLong(1, clientInstance.getId());
			pstmt.setInt(2, clientInstance.getAuth());
			pstmt.setLong(3, clientInstanceId);
			int rowsModified = pstmt.executeUpdate();
			callback.onSuccess(null);
		} catch (SQLException e) {
			callback.onFailure(e);
		} finally {
			tuple.cleanup();
		}
	}

	@Override
	public void getDomainModelDeltaIterator(DeltaApplicationFilters filters,
			final AsyncCallback<Iterator<DomainModelDelta>> callback) {
		String sql = getTransformWrapperSql(filters);
		AsyncCallback<List<DeltaApplicationRecord>> converterCallback = new AsyncCallback<List<DeltaApplicationRecord>>() {
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(List<DeltaApplicationRecord> result) {
				callback.onSuccess(CollectionFilters
						.convert(
								result,
								new DeltaApplicationRecordToDomainModelDeltaConverter())
						.iterator());
			}
		};
		getTransforms(filters, converterCallback);
	}

	@Override
	public void getClientInstanceIdOfDomainObjectDelta(AsyncCallback callback) {
		DeltaApplicationFilters filters = new DeltaApplicationFilters();
		filters.protocolVersion = DomainTrancheProtocolHandler.VERSION;
		getTransforms(filters, callback);
	}

	@Override
	protected void getTransforms(DeltaApplicationFilters filters,
			AsyncCallback<List<DeltaApplicationRecord>> callback) {
		CleanupTuple cleanupTuple = new CleanupTuple();
		List<DeltaApplicationRecord> transforms = new ArrayList<DeltaApplicationRecord>();
		try {
			ResultSet rs = getTransformsResultSet(filters, cleanupTuple);
			while (rs.next()) {
				Map<String, Object> map = getFieldsAs(rs, transformParams);
				DeltaApplicationRecord wr = new DeltaApplicationRecord(
						(Integer) map.get("id"), (String) map.get("transform"),
						(Long) map.get("timestamp"), (Long) map.get("user_id"),
						(Long) map.get("clientInstance_id"),
						(Integer) map.get("request_id"),
						(Integer) map.get("clientInstance_auth"),
						(DeltaApplicationRecordType) map
								.get("transform_request_type"),
						(String) map.get("transform_event_protocol"),
						(String) map.get("tag"));
				transforms.add(wr);
			}
		} catch (Exception e) {
			callback.onFailure(e);
		} finally {
			cleanupTuple.cleanup();
		}
		callback.onSuccess(transforms);
	}
}

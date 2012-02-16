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
package cc.alcina.framework.gwt.persistence.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.code.gwt.database.client.Database;
import com.google.code.gwt.database.client.GenericRow;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLResultSet;
import com.google.code.gwt.database.client.SQLResultSetRowList;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.code.gwt.database.client.StatementCallback;
import com.google.code.gwt.database.client.TransactionCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class WebDatabaseTransformPersistence extends
		LocalTransformPersistenceGwt {
	private Database db;

	public WebDatabaseTransformPersistence() {
	}

	@Override
	public void clearPersistedClient(final ClientInstance exceptFor,
			final PersistenceCallback callback) {
		db.transaction(new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql("DELETE from TransformRequests"
						+ " where (transform_request_type='CLIENT_OBJECT_LOAD'"
						+ " OR transform_request_type='CLIENT_SYNC'"
						+ " OR transform_request_type='TO_REMOTE_COMPLETED')"
						+ " and clientInstance_id != "
						+ (exceptFor == null ? -1 : exceptFor.getId()), null);
			}

			@Override
			public void onTransactionSuccess() {
				callback.onSuccess(null);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				callbackFail(callback, error);
			}
		});
	}
	@Override
	protected void persistOfflineTransforms(
			List<DTRSimpleSerialWrapper> uncommitted,ModalNotifier modalNotifier,
			AsyncCallback<Void> postPersistOfflineTransformsCallback) {
		new PartialDtrUploader().persistOfflineTransforms(uncommitted,modalNotifier,
				postPersistOfflineTransformsCallback);
	}

	@Override
	protected void getTransforms(final DomainTransformRequestType[] types,
			final PersistenceCallback<List<DTRSimpleSerialWrapper>> callback) {
		final Object[] params = { "id", Integer.class, "transform",
				String.class, "timestamp", Long.class, "user_id", Long.class,
				"clientInstance_id", Long.class, "request_id", Integer.class,
				"clientInstance_auth", Integer.class, "transform_request_type",
				DomainTransformRequestType.class, "transform_event_protocol",
				String.class, "tag", String.class };
		final List<DTRSimpleSerialWrapper> transforms = new ArrayList<DTRSimpleSerialWrapper>();
		db.transaction(new TransactionCallback() {
			public void onTransactionStart(SQLTransaction tx) {
				String sql = "select * from TransformRequests ";
				for (int i = 0; i < types.length; i++) {
					sql += i == 0 ? " where (" : " or ";
					sql += CommonUtils.format("transform_request_type='%1'",
							types[i]);
				}
				sql += ") ";
				if (getClientInstanceIdForGet() != null) {
					sql += CommonUtils.format(" and clientInstance_id=%1 ",
							getClientInstanceIdForGet());
				}
				sql += "  order by id asc";
				StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
					@Override
					public void onSuccess(SQLTransaction transaction,
							SQLResultSet<GenericRow> resultSet) {
						SQLResultSetRowList<GenericRow> rs = resultSet
								.getRows();
						for (int i = 0; i < rs.getLength(); i++) {
							Map<String, Object> map = getFieldsAs(
									rs.getItem(i), params);
							DTRSimpleSerialWrapper wr = new DTRSimpleSerialWrapper(
									(Integer) map.get("id"), (String) map
											.get("transform"), (Long) map
											.get("timestamp"), (Long) map
											.get("user_id"), (Long) map
											.get("clientInstance_id"),
									(Integer) map.get("request_id"),
									(Integer) map.get("clientInstance_auth"),
									(DomainTransformRequestType) map
											.get("transform_request_type"),
									(String) map
											.get("transform_event_protocol"),
									(String) map.get("tag"));
							transforms.add(wr);
						}
					}

					@Override
					public boolean onFailure(SQLTransaction transaction,
							SQLError error) {
						return true;
					}
				};
				tx.executeSql(sql, null, okCallback);
			}

			public void onTransactionFailure(SQLError error) {
				callbackFail(callback, error);
			}

			public void onTransactionSuccess() {
				callback.onSuccess(transforms);
			}
		});
	}

	@Override
	public void init(
			final DTESerializationPolicy dteSerializationPolicy,
			final CommitToStorageTransformListener commitToServerTransformListener,
			final PersistenceCallback callback) {
		final LocalTransformPersistence listener = this;
		try {
			db = Database.openDatabase(getTransformDbName(), "1.0",
					"Alcina Transforms", 5000000);
		} catch (Exception e) {
			// squelch - no gears
		}
		setLocalStorageInstalled(db != null);
		if (isLocalStorageInstalled()) {
			final PersistenceCallback superCallback = new PersistenceCallback() {
				@Override
				public void onSuccess(Object result) {
					getCommitToStorageTransformListener()
							.addStateChangeListener(listener);
					ClientTransformManager.cast()
							.setPersistableTransformListener(listener);
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
		}
		callback.onSuccess(null);
	}

	protected String getTransformDbName() {
		return "alcina-transform-persistence";
	}

	private void initSuper(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener,
			final PersistenceCallback superCallback) {
		super.init(dteSerializationPolicy, commitToServerTransformListener,
				superCallback);
	}

	private void ensureDb(final PersistenceCallback callback) {
		TransactionCallback createCallback = new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql(
						"CREATE TABLE IF NOT EXISTS "
								+ "TransformRequests"
								+ " (id INTEGER PRIMARY KEY AUTOINCREMENT,"
								+ " transform TEXT, timestamp INTEGER, user_id INTEGER,"
								+ " clientInstance_id INTEGER, request_id INTEGER,"
								+ "clientInstance_auth INTEGER, transform_request_type nvarchar(255),"
								+ "transform_event_protocol nvarchar(255), tag nvarchar(255))  ",
						null);
			}

			@Override
			public void onTransactionSuccess() {
				callback.onSuccess(null);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				callbackFail(callback, error);
			}
		};
		db.transaction(createCallback);
	}

	private Map<String, Object> getFieldsAs(GenericRow row, Object[] params) {
		Map<String, Object> result = new HashMap<String, Object>();
		int fieldCount = row.getAttributeNames().size();
		for (int i = 0; i < params.length; i += 2) {
			String paramName = (String) params[i];
			Class paramClass = (Class) params[i + 1];
			Object value = null;
			if (paramClass == Long.class) {
				value = (long) row.getDouble(paramName);
			}
			if (paramClass == Integer.class) {
				value = row.getInt(paramName);
			}
			if (paramClass == String.class) {
				value = row.getString(paramName);
			}
			if (paramClass.isEnum()) {
				value = Enum.valueOf(paramClass, row.getString(paramName));
			}
			result.put(paramName, value);
		}
		return result;
	}

	@Override
	protected void clearAllPersisted(final PersistenceCallback callback) {
		db.transaction(new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql("DELETE from TransformRequests", null);
			}

			@Override
			public void onTransactionSuccess() {
				callback.onSuccess(null);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				callbackFail(callback, error);
			}
		});
	}

	@Override
	protected void persist(final DTRSimpleSerialWrapper wrapper,
			final PersistenceCallback callback) {
		if (wrapper.getProtocolVersion() == null) {
			callback.onFailure(new Exception(
					"wrapper must have protocol version"));
		}
		db.transaction(new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql(
						"INSERT INTO TransformRequests "
								+ "(transform, timestamp,"
								+ "user_id,clientInstance_id"
								+ ",request_id,clientInstance_auth,"
								+ "transform_request_type,transform_event_protocol,tag) VALUES (?, ?,?,?,?,?,?,?,?)",
						new String[] {
								wrapper.getText(),
								Long.toString(wrapper.getTimestamp()),
								Long.toString(wrapper.getUserId()),
								Long.toString(wrapper.getClientInstanceId()),
								Long.toString(wrapper.getRequestId()),
								Long.toString(wrapper.getClientInstanceAuth()),
								wrapper.getDomainTransformRequestType()
										.toString(),
								wrapper.getProtocolVersion(), wrapper.getTag() },
						new StatementCallback<GenericRow>() {
							@Override
							public void onSuccess(SQLTransaction transaction,
									SQLResultSet<GenericRow> resultSet) {
								int insertId = resultSet.getInsertId();
								wrapper.setId(insertId);
							}

							@Override
							public boolean onFailure(
									SQLTransaction transaction, SQLError error) {
								return true;
							}
						});
			}

			@Override
			public void onTransactionSuccess() {
				if (wrapper.getDomainTransformRequestType() == DomainTransformRequestType.CLIENT_OBJECT_LOAD) {
					clearPersistedClient(ClientLayerLocator.get()
							.getClientInstance(), callback);
				} else {
					callback.onSuccess(null);
				}
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				callbackFail(callback, error);
			}
		});
	}

	@Override
	protected void transformPersisted(
			final List<DTRSimpleSerialWrapper> persistedWrappers,
			final PersistenceCallback callback) {
		db.transaction(new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				for (DTRSimpleSerialWrapper wrapper : persistedWrappers) {
					tx.executeSql("update  TransformRequests  set "
							+ "transform_request_type='TO_REMOTE_COMPLETED'"
							+ " where id = ?",
							new String[] { Integer.toString(wrapper.getId()) });
				}
			}

			@Override
			public void onTransactionSuccess() {
				callback.onSuccess(null);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				callbackFail(callback, error);
			}
		});
	}

	@Override
	protected void reparentToClientInstance(
			final DTRSimpleSerialWrapper wrapper,
			final ClientInstance clientInstance,
			final PersistenceCallback callback) {
		db.transaction(new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql("update  TransformRequests  set "
						+ "clientInstance_id=?,clientInstance_auth=? "
						+ " where id = ?",
						new String[] { Long.toString(clientInstance.getId()),
								Integer.toString(clientInstance.getAuth()),
								Integer.toString(wrapper.getId()) });
			}

			@Override
			public void onTransactionSuccess() {
				callback.onSuccess(null);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				callbackFail(callback, error);
			}
		});
	}
	
	@Override
	public void reparentToClientInstance(
			final long clientInstanceId,
			final ClientInstance clientInstance, 
			final PersistenceCallback callback) {
		db.transaction(new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql("update  TransformRequests  set "
						+ "clientInstance_id=?,clientInstance_auth=? "
						+ " where id = ?",
						new String[] { Long.toString(clientInstance.getId()),
								Integer.toString(clientInstance.getAuth()),
								Long.toString(clientInstanceId) });
			}

			@Override
			public void onTransactionSuccess() {
				callback.onSuccess(null);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				callbackFail(callback, error);
			}
		});
	}

	public void callbackFail(final PersistenceCallback callback, SQLError error) {
		callback.onFailure(new Exception("Problem initalising webdb - "
				+ error.getMessage() + " - " + error.getCode()));
	}

	@Override
	public String getPersistenceStoreName() {
		return "Html5 web database";
	}
}

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.gwt.persistence.client.DtrWrapperBackedDomainModelDelta.DtrWrapperToDomainModelDeltaConverter;

import com.google.code.gwt.database.client.Database;
import com.google.code.gwt.database.client.GenericRow;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLResultSet;
import com.google.code.gwt.database.client.SQLResultSetRowList;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.totsp.gwittir.client.beans.Converter;

@SuppressWarnings("unchecked")
/**
 *refactor-consort (init method)
 * @author Nick Reddel
 */
public class WebDatabaseTransformPersistence extends
		LocalTransformPersistenceGwt {
	public static final String ALCINA_TRANSFORM_PERSISTENCE = "alcina-transform-persistence";

	public static boolean isStorageQuotaError(SQLError error) {
		return (error.getMessage().contains("storage quota") || error.getCode() == 4);
	}

	private Database db;

	private String transformDatabaseName = ALCINA_TRANSFORM_PERSISTENCE;

	Object[] transformParams = { "id", Integer.class, "transform",
			String.class, "timestamp", Long.class, "user_id", Long.class,
			"clientInstance_id", Long.class, "request_id", Integer.class,
			"clientInstance_auth", Integer.class, "transform_request_type",
			DomainTransformRequestType.class, "transform_event_protocol",
			String.class, "tag", String.class };

	public WebDatabaseTransformPersistence() {
	}

	public WebDatabaseTransformPersistence(String transformDatabaseName) {
		if (transformDatabaseName != null) {
			this.transformDatabaseName = transformDatabaseName;
		}
	}

	public void callbackFail(final AsyncCallback callback, SQLError error) {
		String message = "Problem saving work (web database) - "
				+ error.getMessage() + " - " + error.getCode();
		Exception exception = isStorageQuotaError(error) ? new StorageQuotaException(
				message) : new Exception(message);
		AlcinaTopics.localPersistenceException(exception);
		callback.onFailure(exception);
	}

	@Override
	public void clearPersistedClient(ClientInstance exceptFor, int exceptForId,
			final AsyncCallback callback) {
		String sql = CommonUtils.formatJ("DELETE from TransformRequests"
				+ " where (transform_request_type='CLIENT_OBJECT_LOAD'"
				+ " OR transform_request_type='CLIENT_SYNC'"
				+ " OR transform_request_type='TO_REMOTE_COMPLETED')"
				+ " and (clientInstance_id != %s and id != %s)",
				exceptFor == null ? -1 : exceptFor.getId(), exceptForId);
		executeSql(sql, callback);
	}

	public Database getDb() {
		return this.db;
	}

	@Override
	public void getDomainModelDeltaIterator(DomainTransformRequestType[] types,
			AsyncCallback<Iterator<DomainModelDelta>> callback) {
		String sql = getTransformWrapperSql(types);
		db.transaction(new ListTransformsAsDeltasCallback(sql, callback));
	}

	@Override
	public String getPersistenceStoreName() {
		return "Html5 web database";
	};

	@Override
	public void init(
			final DTESerializationPolicy dteSerializationPolicy,
			final CommitToStorageTransformListener commitToServerTransformListener,
			final AsyncCallback callback) {
		final LocalTransformPersistence listener = this;
		try {
			db = Database.openDatabase(getTransformDbName(), "1.0",
					"Alcina Transforms", 5000000);
		} catch (Exception e) {
			callback.onFailure(e);
			return;
		}
		setLocalStorageInstalled(db != null);
		if (isLocalStorageInstalled()) {
			final AsyncCallback superCallback = new AsyncCallback() {
				@Override
				public void onFailure(Throwable caught) {
					setLocalStorageInstalled(false);
					callback.onFailure(caught);
				}

				@Override
				public void onSuccess(Object result) {
					getCommitToStorageTransformListener()
							.addStateChangeListener(listener);
					ClientTransformManager.cast()
							.setPersistableTransformListener(listener);
					callback.onSuccess(null);
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
		} else {
			callback.onSuccess(null);
		}
	}

	@Override
	public void reparentToClientInstance(DTRSimpleSerialWrapper wrapper,
			ClientInstance clientInstance, AsyncCallback callback) {
		String sql = "update  TransformRequests  set "
				+ "clientInstance_id=?,clientInstance_auth=? "
				+ " where id = ?";
		executeSql(sql, callback, Long.toString(clientInstance.getId()),
				Integer.toString(clientInstance.getAuth()),
				Integer.toString(wrapper.getId()));
	}

	private void ensureDb(final AsyncCallback callback) {
		String sql = "CREATE TABLE IF NOT EXISTS "
				+ "TransformRequests"
				+ " (id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ " transform TEXT, timestamp INTEGER, user_id INTEGER,"
				+ " clientInstance_id INTEGER, request_id INTEGER,"
				+ "clientInstance_auth INTEGER, transform_request_type nvarchar(255),"
				+ "transform_event_protocol nvarchar(255), tag nvarchar(255))  ";
		executeSql(sql, callback);
	}

	private void executeSql(String sql, AsyncCallback callback) {
		executeSql(sql, callback, (Object[]) null);
	}

	private void executeSql(String sql, AsyncCallback callback,
			Object... arguments) {
		db.transaction(new ExecSqlPersistenceHandler(sql, callback, arguments));
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

	private void initSuper(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener,
			final AsyncCallback superCallback) {
		super.init(dteSerializationPolicy, commitToServerTransformListener,
				superCallback);
	}

	@Override
	protected void clearAllPersisted(final AsyncCallback callback) {
		executeSql("DELETE from TransformRequests", callback);
	}

	protected String getTransformDbName() {
		return transformDatabaseName;
	}

	@Override
	protected void getTransforms(final DomainTransformRequestType[] types,
			final AsyncCallback<List<DTRSimpleSerialWrapper>> callback) {
		String sql = getTransformWrapperSql(types);
		db.transaction(new ListTransformWrappersCallback(sql, callback));
	}

	private String getTransformWrapperSql(
			final DomainTransformRequestType[] types) {
		String sql = "select * from TransformRequests ";
		for (int i = 0; i < types.length; i++) {
			sql += i == 0 ? " where (" : " or ";
			sql += CommonUtils.formatJ("transform_request_type='%s'", types[i]);
		}
		sql += ") ";
		if (getClientInstanceIdForGet() != null) {
			sql += CommonUtils.formatJ(" and clientInstance_id=%s ",
					getClientInstanceIdForGet());
		}
		sql += "  order by id asc";
		return sql;
	}

	@Override
	protected void persist(final DTRSimpleSerialWrapper wrapper,
			final AsyncCallback callback) {
		notifyPersisting(new TypeSizeTuple(wrapper
				.getDomainTransformRequestType().toString(), wrapper.getText()
				.length()));
		if (wrapper.getDomainTransformRequestType() == DomainTransformRequestType.TO_REMOTE) {
			AlcinaTopics.logCategorisedMessage(new StringPair(
					AlcinaTopics.LOG_CATEGORY_TRANSFORM, wrapper.getText()));
		}
		maybeCompressWrapper(wrapper);
		if (wrapper.getProtocolVersion() == null) {
			callback.onFailure(new Exception(
					"wrapper must have protocol version"));
		}
		// fw3 - before running (in persist postload), clear existing -- ipad
		// space
		// @Override
		// public void onTransactionSuccess() {
		// if (wrapper.getDomainTransformRequestType() ==
		// DomainTransformRequestType.CLIENT_OBJECT_LOAD) {
		// clearPersistedClient(ClientLayerLocator.get()
		// .getClientInstance(), callback);
		// } else {
		// callback.onSuccess(null);
		// }
		// }
		db.transaction(new PersistTransformsHandler(callback, wrapper));
	}

	@Override
	protected void persistOfflineTransforms(
			List<DTRSimpleSerialWrapper> uncommitted,
			ModalNotifier modalNotifier,
			AsyncCallback<Void> postPersistOfflineTransformsCallback) {
		new PartialDtrUploader().persistOfflineTransforms(uncommitted,
				modalNotifier, postPersistOfflineTransformsCallback);
	}

	@Override
	protected void transformPersisted(
			final List<DTRSimpleSerialWrapper> persistedWrappers,
			final AsyncCallback callback) {
		Converter<DTRSimpleSerialWrapper, Integer> getIdConverter = new Converter<DTRSimpleSerialWrapper, Integer>() {
			@Override
			public Integer convert(DTRSimpleSerialWrapper original) {
				return original.getId();
			}
		};
		List<Integer> ids = CollectionFilters.convert(persistedWrappers,
				getIdConverter);
		executeSql(CommonUtils.formatJ("update  TransformRequests  set "
				+ "transform_request_type='TO_REMOTE_COMPLETED'"
				+ " where id in (%s)", CommonUtils.join(ids, ", ")), callback);
	}

	public static class StorageQuotaException extends Exception {
		public StorageQuotaException(String message) {
			super(message);
		}
	}

	class ExecSqlPersistenceHandler<T> extends
			FailureDecoratedPersistenceHandler<T> {
		public ExecSqlPersistenceHandler(String sql,
				AsyncCallback<T> postTransactionCallback) {
			super(sql, postTransactionCallback);
		}

		public ExecSqlPersistenceHandler(String sql,
				AsyncCallback<T> postTransactionCallback, Object... arguments) {
			super(sql, postTransactionCallback, arguments);
		}

		@Override
		public void onSuccess(SQLTransaction transaction,
				SQLResultSet<GenericRow> resultSet) {
		}

		@Override
		protected T getResult() {
			return null;
		}
	}

	abstract class FailureDecoratedPersistenceHandler<T> extends
			WebdbSingleSqlStatementPersistenceHandler<T> {
		public FailureDecoratedPersistenceHandler(String sql,
				AsyncCallback<T> postTransactionCallback) {
			super(sql, postTransactionCallback);
		}

		public FailureDecoratedPersistenceHandler(String sql,
				AsyncCallback<T> postTransactionCallback, Object[] arguments) {
			super(sql, postTransactionCallback, arguments);
		}

		@Override
		public void onTransactionFailure(SQLError error) {
			callbackFail(postTransactionCallback,
					statementError != null ? statementError : error);
		}
	}

	abstract class ListTransformsCallback<T> extends
			FailureDecoratedPersistenceHandler<T> {
		final List<DTRSimpleSerialWrapper> transforms = new ArrayList<DTRSimpleSerialWrapper>();

		public ListTransformsCallback(String sql,
				AsyncCallback<T> postTransactionCallback) {
			super(sql, postTransactionCallback);
		}

		@Override
		public void onSuccess(SQLTransaction transaction,
				SQLResultSet<GenericRow> resultSet) {
			SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
			for (int i = 0; i < rs.getLength(); i++) {
				Map<String, Object> map = getFieldsAs(rs.getItem(i),
						transformParams);
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
				maybeDecompressWrapper(wr);
				transforms.add(wr);
			}
		}
	}

	class ListTransformWrappersCallback extends
			ListTransformsCallback<List<DTRSimpleSerialWrapper>> {
		public ListTransformWrappersCallback(
				String sql,
				AsyncCallback<List<DTRSimpleSerialWrapper>> postTransactionCallback) {
			super(sql, postTransactionCallback);
		}

		@Override
		protected List<DTRSimpleSerialWrapper> getResult() {
			return transforms;
		}
	}

	class PersistTransformsHandler extends
			FailureDecoratedPersistenceHandler<Void> {
		private DTRSimpleSerialWrapper wrapper;

		private int persistSpacePass;

		public PersistTransformsHandler(
				AsyncCallback<Void> postTransactionCallback,
				DTRSimpleSerialWrapper wrapper) {
			super(null, postTransactionCallback);
			this.wrapper = wrapper;
		}

		@Override
		public void onSuccess(SQLTransaction transaction,
				SQLResultSet<GenericRow> resultSet) {
			int insertId = resultSet.getInsertId();
			wrapper.setId(insertId);
		}

		@Override
		public void onTransactionFailure(SQLError error) {
			if (isStorageQuotaError(error) && persistSpacePass++ <= 3) {
				db.transaction(this);
				return;
			}
			callbackFail(postTransactionCallback, error);
		}

		public void onTransactionStart(SQLTransaction tx) {
			String sql = "INSERT INTO TransformRequests "
					+ "(transform, timestamp,"
					+ "user_id,clientInstance_id"
					+ ",request_id,clientInstance_auth,"
					+ "transform_request_type,transform_event_protocol,tag) VALUES (?, ?,?,?,?,?,?,?,?)";
			String[] arguments = new String[] { wrapper.getText(),
					Long.toString(wrapper.getTimestamp()),
					Long.toString(wrapper.getUserId()),
					Long.toString(wrapper.getClientInstanceId()),
					Long.toString(wrapper.getRequestId()),
					Long.toString(wrapper.getClientInstanceAuth()),
					wrapper.getDomainTransformRequestType().toString(),
					wrapper.getProtocolVersion(), wrapper.getTag() };
			tx.executeSql(sql, arguments, this);
		}

		@Override
		protected Void getResult() {
			return null;
		}
	}

	class ListTransformsAsDeltasCallback extends
			ListTransformsCallback<Iterator<DomainModelDelta>> {
		public ListTransformsAsDeltasCallback(
				String sql,
				AsyncCallback<Iterator<DomainModelDelta>> postTransactionCallback) {
			super(sql, postTransactionCallback);
		}

		Iterator<DomainModelDelta> iterator = null;

		@Override
		protected Iterator<DomainModelDelta> getResult() {
			if (iterator == null) {
				iterator = CollectionFilters.convert(transforms,
						new DtrWrapperToDomainModelDeltaConverter()).iterator();
			}
			return iterator;
		}
	}
}

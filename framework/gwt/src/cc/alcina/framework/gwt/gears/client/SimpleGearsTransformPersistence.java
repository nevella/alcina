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

package cc.alcina.framework.gwt.gears.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformRequest.DataTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.ClientWorker;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.PersistableTransformListener;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CommitToServerTransformListener;
import cc.alcina.framework.gwt.client.widget.dialog.CancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;

import com.google.gwt.gears.client.Factory;
import com.google.gwt.gears.client.GearsException;
import com.google.gwt.gears.client.database.Database;
import com.google.gwt.gears.client.database.ResultSet;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
@SuppressWarnings("unchecked")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class SimpleGearsTransformPersistence implements StateChangeListener,
		PersistableTransformListener {
	private SimpleGearsTransformPersistence() {
		super();
	}

	private boolean gearsInstalled = false;

	public boolean isGearsInstalled() {
		return this.gearsInstalled;
	}

	private static SimpleGearsTransformPersistence theInstance;

	public static SimpleGearsTransformPersistence get() {
		if (theInstance == null) {
			theInstance = new SimpleGearsTransformPersistence();
		}
		return theInstance;
	}

	private CommitToServerTransformListener commitToServerTransformListener;

	private Factory factory;

	private Database db;

	public void appShutdown() {
		theInstance = null;
	}

	public void setCommitToServerTransformListener(
			CommitToServerTransformListener serverTransformListener) {
		this.commitToServerTransformListener = serverTransformListener;
		try {
			factory = Factory.getInstance();
		} catch (Exception e) {
			// squelch - no gears
		}
		gearsInstalled = factory != null;
		if (gearsInstalled) {
			try {
				createDb();
				serverTransformListener.addStateChangeListener(this);
				TransformManager.get().setPersistableTransformListener(this);
			} catch (Exception e) {
				gearsInstalled = false;
				// squelch - no gears
			}
		}
	}

	private void createDb() {
		db = Factory.getInstance().createDatabase();
		db.open("dtr-persistence");
		try {
			db
					.execute("CREATE TABLE IF NOT EXISTS "
							+ "TransformRequests"
							+ " (id INTEGER PRIMARY KEY AUTOINCREMENT,"
							+ " transform TEXT, timestamp INTEGER, user_id INTEGER, clientInstance_id INTEGER)");
		} catch (Exception e) {
			throw new WrappedRuntimeException("Problem accessing gears db", e,
					SuggestedAction.NOTIFY_WARNING);
		}
		try {
			db
					.execute("ALTER TABLE TransformRequests add column request_id INTEGER");
			db
					.execute("ALTER TABLE TransformRequests add column clientInstance_auth INTEGER");
			db
					.execute("update TransformRequests set request_id=0,clientInstance_auth=0");
		} catch (Exception e) {
		}
		try {
			db
					.execute("ALTER TABLE TransformRequests add column transform_request_type nvarchar(255)");
			db
					.execute("update TransformRequests set transform_request_type='TO_REMOTE'");
		} catch (Exception e) {
		}
		try {
			db
					.execute("ALTER TABLE TransformRequests add column session_id nvarchar(255)");
			db.execute("update TransformRequests set session_id=null");
		} catch (Exception e) {
		}
	}

	public CommitToServerTransformListener getCommitToServerTransformListener() {
		return commitToServerTransformListener;
	}

	public void stateChanged(Object source, String newState) {
		if (newState == CommitToServerTransformListener.COMMITTING) {
			List<DataTransformRequest> rqs = commitToServerTransformListener
					.getPriorRequestsWithoutResponse();
			for (DataTransformRequest rq : rqs) {
				int requestId = rq.getRequestId();
				if (!persistedTransforms.containsKey(requestId)
						&& !rq.getItems().isEmpty()) {
					DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(
							rq);
					persist(wrapper);
					persistedTransforms.put(requestId, wrapper);
				}
			}
		} else if (newState == CommitToServerTransformListener.COMMITTED) {
			List<DataTransformRequest> rqs = commitToServerTransformListener
					.getPriorRequestsWithoutResponse();
			Set<Integer> removeIds = new HashSet(persistedTransforms.keySet());
			for (DataTransformRequest rq : rqs) {
				removeIds.remove(rq.getRequestId());
			}
			for (Integer i : removeIds) {
				DTRSimpleSerialWrapper wrapper = persistedTransforms.get(i);
				transformPersisted(wrapper);
				persistedTransforms.remove(i);
			}
			DataTransformRequest rq = new DataTransformRequest();
			rq.setClientInstance(commitToServerTransformListener.getClientInstance());
			rq
					.setDataTransformRequestType(DataTransformRequestType.CLIENT_SYNC);
			rq.setRequestId(0);
			rq.setItems(new ArrayList<DataTransformEvent>(
					commitToServerTransformListener.getSynthesisedEvents()));
			DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(rq);
			persist(wrapper);
		}
	}

	public Map<Integer, DTRSimpleSerialWrapper> getPersistedTransforms() {
		return this.persistedTransforms;
	}

	private void transformPersisted(DTRSimpleSerialWrapper wrapper) {
		try {
			db.execute("update  TransformRequests  set "
					+ "transform_request_type='TO_REMOTE_COMPLETED'"
					+ " where id = ?", new String[] { Integer.toString(wrapper
					.getId()) });
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void persist(DTRSimpleSerialWrapper wrapper) {
		try {
			db
					.execute(
							"INSERT INTO TransformRequests "
									+ "(transform, timestamp,"
									+ "user_id,clientInstance_id"
									+ ",request_id,clientInstance_auth,"
									+ "transform_request_type,session_id) VALUES (?, ?,?,?,?,?,?,?)",
							new String[] {
									wrapper.getText(),
									Long.toString(wrapper.getTimestamp()),
									Long.toString(wrapper.getUserId()),
									Long
											.toString(wrapper
													.getClientInstanceId()),
									Long.toString(wrapper.getRequestId()),
									Long.toString(wrapper
											.getClientInstanceAuth()),
									wrapper.getDataTransformRequestType()
											.toString(),
									closing ? null : ClientSession.get()
											.getSessionId() });
			if (wrapper.getDataTransformRequestType() == DataTransformRequestType.CLIENT_OBJECT_LOAD) {
				clearPersistedClient(commitToServerTransformListener
						.getClientInstance());
			}
			wrapper.setId(db.getLastInsertRowId());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private Map<Integer, DTRSimpleSerialWrapper> persistedTransforms = new HashMap<Integer, DTRSimpleSerialWrapper>();

	private int getFieldIndex(ResultSet rs, String fieldName) throws Exception {
		int fieldCount = rs.getFieldCount();
		for (int i = 0; i < fieldCount; i++) {
			if (rs.getFieldName(i).equals(fieldName)) {
				return i;
			}
		}
		throw new WrappedRuntimeException("Field name not found: " + fieldName,
				SuggestedAction.NOTIFY_WARNING);
	}

	private Map<String, Object> getFieldsAs(ResultSet rs, Object[] params)
			throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		int fieldCount = rs.getFieldCount();
		for (int i = 0; i < params.length; i += 2) {
			String paramName = (String) params[i];
			Class paramClass = (Class) params[i + 1];
			Object value = null;
			if (paramClass == Long.class) {
				value = rs.getFieldAsLong(getFieldIndex(rs, paramName));
			}
			if (paramClass == Integer.class) {
				value = rs.getFieldAsInt(getFieldIndex(rs, paramName));
			}
			if (paramClass == String.class) {
				value = rs.getFieldAsString(getFieldIndex(rs, paramName));
			}
			if (paramClass.isEnum()) {
				value = Enum.valueOf(paramClass, rs
						.getFieldAsString(getFieldIndex(rs, paramName)));
			}
			result.put(paramName, value);
		}
		return result;
	}

	private List<DTRSimpleSerialWrapper> getTransforms(
			DataTransformRequestType type) throws Exception {
		return getTransforms(new DataTransformRequestType[] { type });
	}

	private Long clientInstanceIdForGet = null;

	public List<DTRSimpleSerialWrapper> getTransforms(
			DataTransformRequestType[] types) throws Exception {
		Object[] params = { "id", Integer.class, "transform", String.class,
				"timestamp", Long.class, "user_id", Long.class,
				"clientInstance_id", Long.class, "request_id", Integer.class,
				"clientInstance_auth", Integer.class, "transform_request_type",
				DataTransformRequestType.class };
		List<DTRSimpleSerialWrapper> transforms = new ArrayList<DTRSimpleSerialWrapper>();
		String sql = "select * from TransformRequests ";
		for (int i = 0; i < types.length; i++) {
			sql += i == 0 ? " where (" : " or ";
			sql += CommonUtils.format("transform_request_type='%1'", types[i]);
		}
		sql += ") ";
		if (clientInstanceIdForGet != null) {
			sql += CommonUtils.format(" and session_id='%1' ", ClientSession
					.get().getSessionId());
			sql += CommonUtils.format(" and clientInstance_id=%1 ",
					clientInstanceIdForGet);
		}
		sql += "  order by id asc";
		ResultSet rs = db.execute(sql);
		for (int i = 0; rs.isValidRow(); ++i, rs.next()) {
			Map<String, Object> map = getFieldsAs(rs, params);
			DTRSimpleSerialWrapper wr = new DTRSimpleSerialWrapper(
					(Integer) map.get("id"), (String) map.get("transform"),
					(Long) map.get("timestamp"), (Long) map.get("user_id"),
					(Long) map.get("clientInstance_id"), (Integer) map
							.get("request_id"), (Integer) map
							.get("clientInstance_auth"),
					(DataTransformRequestType) map
							.get("transform_request_type"));
			transforms.add(wr);
		}
		rs.close();
		return transforms;
	}

	public void handleUncommittedTransformsOnLoad(final Callback cb) {
		if (!gearsInstalled) {
			cb.callback(null);
			return;
		}
		try {
			final List<DTRSimpleSerialWrapper> uncommitted = getTransforms(DataTransformRequestType.TO_REMOTE);
			if (!uncommitted.isEmpty()) {
				final CancellableRemoteDialog crd = new NonCancellableRemoteDialog(
						"Saving unsaved work from previous session", null);
				crd.getGlass().setOpacity(0);
				AsyncCallback<Void> callback = new AsyncCallback<Void>() {
					private void hideDialog() {
						crd.hide();
					}

					public void onSuccess(Void result) {
						hideDialog();
						clearPersisted();
						Window
								.alert("Save work from previous session to server completed");
						cb.callback(null);
					}

					public void onFailure(Throwable caught) {
						hideDialog();
						new SimpleConflictResolver().resolve(uncommitted,caught,SimpleGearsTransformPersistence.this,cb);
						
					}
				};
				crd.show();
				ClientLayerLocator.get().commonRemoteServiceAsync()
						.persistOfflineTransforms(uncommitted, callback);
				return;
			} else {
				cb.callback(null);
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void clearPersisted() {
		try {
			db.execute("DELETE from TransformRequests");
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void clearPersistedClient(ClientInstance exceptFor) {
		try {
			db.execute("DELETE from TransformRequests"
					+ " where (transform_request_type='CLIENT_OBJECT_LOAD'"
					+ " OR transform_request_type='CLIENT_SYNC'"
					+ " OR transform_request_type='TO_REMOTE_COMPLETED')"
					+ " and clientInstance_id != " + exceptFor.getId());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private boolean closing = false;

	public void persistableTransform(DataTransformRequest dtr) {
		if (!dtr.getItems().isEmpty()) {
			if (!closing) {
				new DTRAsyncSerializer(dtr).start();
			} else {
				DTRSimpleSerialWrapper wrapper = new DTRSimpleSerialWrapper(
						dtr, false);
				persist(wrapper);
			}
		}
	}

	class DTRAsyncSerializer extends ClientWorker {
		DTRSimpleSerialWrapper wrapper;

		StringBuffer sb = new StringBuffer();

		private List<DataTransformEvent> items;

		public DTRAsyncSerializer(DataTransformRequest dtr) {
			super(1000, 200);
			wrapper = new DTRSimpleSerialWrapper(dtr, true);
			items = dtr.getItems();
		}

		@Override
		protected void performIteration() {
			int max = Math.min(index + iterationCount, items.size());
			StringBuffer sb2 = new StringBuffer();
			lastPassIterationsPerformed = max - index;
			for (; index < max; index++) {
				items.get(index).appendTo(sb2);
			}
			sb.append(sb2.toString());
		}

		@Override
		protected boolean isComplete() {
			return index == items.size();
		}

		protected void onComplete() {
			ClientLayerLocator.get().clientBase().metricLogStart("persist");
			wrapper.setText(sb.toString());
			persist(wrapper);
			ClientLayerLocator.get().clientBase().metricLogEnd("persist");
		}
	}

	public void closeSession(ClientInstance clientInstance) {
		if (!gearsInstalled) {
			return;
		}
		modifySessionIdOfTransforms(clientInstance.getId(), null);
	}

	private void modifySessionIdOfTransforms(Long clientInstanceId,
			String sessionId) {
		try {
			db.execute("update TransformRequests set"
					+ " session_id=? where clientInstance_id=? ", new String[] {
					sessionId, String.valueOf(clientInstanceId) });
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	protected void showOfflineLimitMessage() {
		ClientLayerLocator.get().clientBase().showError(
				"Unable to open offline session",
				new Exception("Only one tab may be open "
						+ "for this application when opening offline. "));
	}

	public List<DTRSimpleSerialWrapper> openAvailableSessionTransformsForOfflineLoad() {
		try {
			List<DTRSimpleSerialWrapper> transforms = new ArrayList<DTRSimpleSerialWrapper>();
			if (hasOpenSessions()) {
				showOfflineLimitMessage();
				return transforms;
			}
			List<DTRSimpleSerialWrapper> loads = getTransforms(DataTransformRequestType.CLIENT_OBJECT_LOAD);
			if (loads.size() == 0) {
				// should never happen (or very rarely)
				showUnableToLoadOfflineMessage();
				return transforms;
			}
			if (loads.size() != 1) {
				// an assert?
				throw new WrappedRuntimeException(
						"Multiple client object loads",
						SuggestedAction.NOTIFY_WARNING);
			}
			DTRSimpleSerialWrapper loadWrapper = loads.iterator().next();
			clientInstanceIdForGet = loadWrapper.getClientInstanceId();
			// lock these transforms
			modifySessionIdOfTransforms(clientInstanceIdForGet, ClientSession
					.get().getSessionId());
			transforms.add(loadWrapper);
			transforms.addAll(getTransforms(new DataTransformRequestType[] {
					DataTransformRequestType.TO_REMOTE_COMPLETED,
					DataTransformRequestType.TO_REMOTE,
					DataTransformRequestType.CLIENT_SYNC }));
			clientInstanceIdForGet = null;
			return transforms;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void showUnableToLoadOfflineMessage() {
		ClientLayerLocator.get().clientBase().showMessage(
				"<b>Unable to open offline session</b><br><br>"
						+ "No data saved");
	}

	private boolean hasOpenSessions() throws GearsException {
		ResultSet rs = db.execute(
				"SELECT count(id) from TransformRequests where session_id=?",
				new String[] { ClientSession.get().getSessionId() });
		boolean result = rs.getFieldAsInt(0) != 0;
		rs.close();
		return result;
	}

	public boolean shouldPersistClient() throws GearsException {
		ClientLayerLocator.get().clientBase().log(
				"Check persistence - has open sessions? " + hasOpenSessions());
		return !hasOpenSessions();
	}

	public void setClosing(boolean closing) {
		this.closing = closing;
	}

	// sometimes seems to be a sync problem (where the last dtwrapper written,
	// on close-flush, was written after 'close session)
	public boolean isClosing() {
		return closing;
	}
}

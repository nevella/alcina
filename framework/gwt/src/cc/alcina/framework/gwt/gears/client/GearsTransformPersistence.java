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
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;

import com.google.gwt.gears.client.Factory;
import com.google.gwt.gears.client.database.Database;
import com.google.gwt.gears.client.database.ResultSet;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class GearsTransformPersistence extends LocalTransformPersistenceGwt {
	private Factory factory;

	private Database db;

	public GearsTransformPersistence() {
	}

	public void clearPersistedClient(ClientInstance exceptFor) {
		try {
			db.execute("DELETE from TransformRequests"
					+ " where (transform_request_type='CLIENT_OBJECT_LOAD'"
					+ " OR transform_request_type='CLIENT_SYNC'"
					+ " OR transform_request_type='TO_REMOTE_COMPLETED')"
					+ " and clientInstance_id != "
					+ (exceptFor == null ? -1 : exceptFor.getId()));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	protected List<DTRSimpleSerialWrapper> getTransforms(
			DomainTransformRequestType[] types) throws Exception {
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
		ResultSet rs = db.execute(sql);
		for (int i = 0; rs.isValidRow(); ++i, rs.next()) {
			Map<String, Object> map = getFieldsAs(rs, params);
			DTRSimpleSerialWrapper wr = new DTRSimpleSerialWrapper(
					(Integer) map.get("id"), (String) map.get("transform"),
					(Long) map.get("timestamp"), (Long) map.get("user_id"),
					(Long) map.get("clientInstance_id"), (Integer) map
							.get("request_id"), (Integer) map
							.get("clientInstance_auth"),
					(DomainTransformRequestType) map
							.get("transform_request_type"), (String) map
							.get("transform_event_protocol"), (String) map
							.get("tag"));
			transforms.add(wr);
		}
		rs.close();
		return transforms;
	}

	@Override
	public void init(DTESerializationPolicy dteSerializationPolicy,
			CommitToStorageTransformListener commitToServerTransformListener) {
		try {
			factory = Factory.getInstance();
		} catch (Exception e) {
			// squelch - no gears
		}
		setLocalStorageInstalled(factory != null);
		if (isLocalStorageInstalled()) {
			try {
				ensureDb();
				super.init(dteSerializationPolicy,
						commitToServerTransformListener);
				getCommitToStorageTransformListener().addStateChangeListener(
						this);
				ClientTransformManager.cast().setPersistableTransformListener(
						this);
			} catch (Exception e) {
				setLocalStorageInstalled(false);
				// squelch - no gears
			}
		}
	}

	private void ensureDb() {
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
					.execute("ALTER TABLE TransformRequests add column transform_event_protocol nvarchar(255)");
		} catch (Exception e) {
		}
		try {
			db
					.execute("ALTER TABLE TransformRequests add column tag nvarchar(255)");
		} catch (Exception e) {
		}
	}

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

	protected void clearAllPersisted() {
		try {
			db.execute("DELETE from TransformRequests");
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void persist(DTRSimpleSerialWrapper wrapper) {
		try {
			if (wrapper.getProtocolVersion() == null) {
				throw new Exception("wrapper must have protocol version");
			}
			db
					.execute(
							"INSERT INTO TransformRequests "
									+ "(transform, timestamp,"
									+ "user_id,clientInstance_id"
									+ ",request_id,clientInstance_auth,"
									+ "transform_request_type,transform_event_protocol,tag) VALUES (?, ?,?,?,?,?,?,?,?)",
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
									wrapper.getDomainTransformRequestType()
											.toString(),
									wrapper.getProtocolVersion(),
									wrapper.getTag() });
			if (wrapper.getDomainTransformRequestType() == DomainTransformRequestType.CLIENT_OBJECT_LOAD) {
				clearPersistedClient(ClientLayerLocator.get()
						.getClientInstance());
			}
			wrapper.setId(db.getLastInsertRowId());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void transformPersisted(DTRSimpleSerialWrapper wrapper) {
		try {
			db.execute("update  TransformRequests  set "
					+ "transform_request_type='TO_REMOTE_COMPLETED'"
					+ " where id = ?", new String[] { Integer.toString(wrapper
					.getId()) });
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	protected void reparentToClientInstance(DTRSimpleSerialWrapper wrapper,
			ClientInstance clientInstance) {
		try {
			db.execute("update  TransformRequests  set "
					+ "clientInstance_id=?,clientInstance_auth=? "
					+ " where id = ?", new String[] {
					Long.toString(clientInstance.getId()),
					Integer.toString(clientInstance.getAuth()),
					Integer.toString(wrapper.getId()) });
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}

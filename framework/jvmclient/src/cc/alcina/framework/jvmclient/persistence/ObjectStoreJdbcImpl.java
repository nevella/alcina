package cc.alcina.framework.jvmclient.persistence;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.util.DiscardInfoWrappingCallback;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;
import cc.alcina.framework.gwt.persistence.client.PersistenceObjectStore;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class ObjectStoreJdbcImpl implements PersistenceObjectStore {
	private String tableName;

	private AsyncCallback<Void> postInitCallback;

	private Connection conn;

	public ObjectStoreJdbcImpl(Connection conn, String tableName,
			AsyncCallback<Void> postInitCallback) {
		this.conn = conn;
		this.tableName = tableName;
		this.postInitCallback = postInitCallback;
		ensureTable();
	}

	@Override
	public void add(String key, String value, AsyncCallback<Integer> idCallback) {
		new PutHandler().put(StringMap.property(key, value), idCallback, true,
				null);
	}

	@Override
	public void clear(final AsyncCallback<Void> callback) {
		executeSql(CommonUtils.formatJ("Delete from %s", tableName), callback);
	}

	@Override
	public void drop(final AsyncCallback<Void> callback) {
		executeSql(CommonUtils.formatJ("DROP TABLE %s", tableName), callback);
	}

	public void executeSql(final String sql, final AsyncCallback callback) {
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			close(stmt);
			callback.onSuccess(null);
		} catch (Exception e) {
			callback.onFailure(e);
		}
	}

	private void close(Statement stmt) {
		try {
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void get(List<String> keys, AsyncCallback<StringMap> valueCallback) {
		new GetHandler().get(keys, valueCallback);
	}

	@Override
	public void get(String key, final AsyncCallback<String> valueCallback) {
		AsyncCallback<StringMap> toSingleStringCallback = new AsyncCallbackStd<StringMap>() {
			@Override
			public void onSuccess(StringMap result) {
				valueCallback.onSuccess(result.isEmpty() ? null : result
						.values().iterator().next());
			}
		};
		new GetHandler().get(Collections.singletonList(key),
				toSingleStringCallback);
	}

	@Override
	public void getIdRange(AsyncCallback<IntPair> completedCallback) {
		new GetIdRageHandler().get(completedCallback);
	}

	@Override
	public void getKeysPrefixedBy(String keyPrefix,
			AsyncCallback<List<String>> completedCallback) {
		new GetPrefixedHandler().get(keyPrefix, completedCallback);
	}

	@Override
	public void getRange(int fromId, int toId,
			AsyncCallback<Map<Integer, String>> valueCallback) {
		new GetRangeHandler().getRange(fromId, toId, valueCallback);
	}

	@Override
	public void put(int id, String value, AsyncCallback<Void> idCallback) {
		new PutHandler()
				.put(StringMap.property(null, value),
						new DiscardInfoWrappingCallback<Integer>(idCallback),
						false, id);
	}

	@Override
	public void put(String key, String value, AsyncCallback<Integer> idCallback) {
		new PutHandler().put(StringMap.property(key, value), idCallback, false,
				null);
	}

	@Override
	public void put(StringMap kvs, AsyncCallback completedCallback) {
		new PutHandler().put(kvs, completedCallback, false, null);
	}

	@Override
	public void remove(List<String> keys, AsyncCallback completedCallback) {
		new RemoveHandler().remove(keys, completedCallback);
	}

	@Override
	public void remove(String key, AsyncCallback<Integer> idCallback) {
		new RemoveHandler().remove(Collections.singletonList(key), idCallback);
	}

	@Override
	/**
	 * Note - inclusive int range - i.e. [1,1] will result in the removal of [1]
	 */
	public void removeIdRange(IntPair range,
			AsyncCallback<Void> completedCallback) {
		new RemoveRangeHandler().removeRange(range.i1, range.i2,
				completedCallback);
	}

	private void ensureTable() {
		try {
			Statement stmt = conn.createStatement();
			stmt.executeQuery(CommonUtils.formatJ("select min(id) from %s",
					tableName));
			postInitCallback.onSuccess(null);
		} catch (Exception e) {
			String createSql = "CREATE TABLE  "
					+ "%s"
					+ " (id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1,\n"
					+ " INCREMENT BY 1) ,\n"
					+ " key_ varchar(255), value_ CLOB)  ";
			String sql = CommonUtils.formatJ(createSql, tableName);
			executeSql(sql, postInitCallback);
		}
	}

	class GetHandler {
		public void get(List<String> keys,
				AsyncCallback<StringMap> valueCallback) {
			try {
				String sql = CommonUtils.formatJ(
						"select key_, value_ from %s where key_ in %s",
						tableName,
						LocalTransformPersistence.stringListToClause(keys));
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				StringMap getResult = new StringMap();
				while (rs.next()) {
					String value = getValueClob(rs);
					getResult.put(rs.getString("key_"), value);
				}
				close(stmt);
				valueCallback.onSuccess(getResult);
			} catch (Exception e) {
				valueCallback.onFailure(e);
			}
		}
	}

	protected String getValueClob(ResultSet rs) throws SQLException,
			IOException {
		String value = null;
		Clob clob = rs.getClob("value_");
		if (clob != null) {
			Reader reader = clob.getCharacterStream();
			char[] cbuf = new char[8192];
			StringBuilder sb = new StringBuilder((int) clob.length());
			while (true) {
				int read = reader.read(cbuf);
				if (read == -1) {
					break;
				}
				sb.append(new String(cbuf, 0, read));
			}
			reader.close();
			value = sb.toString();
		}
		return value;
	}

	class GetIdRageHandler {
		public void get(AsyncCallback<IntPair> valueCallback) {
			try {
				String sql = CommonUtils.formatJ("select min(id) as min_, "
						+ "max(id) as max_ from %s", tableName);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				StringMap getResult = new StringMap();
				rs.next();
				IntPair intPair = new IntPair();
				intPair.i1 = rs.getInt("min_");
				intPair.i2 = rs.getInt("max_");
				close(stmt);
				valueCallback.onSuccess(intPair);
			} catch (SQLException e) {
				valueCallback.onFailure(e);
			}
		}
	}

	class GetPrefixedHandler {
		public void get(String keyPrefix,
				AsyncCallback<List<String>> valueCallback) {
			try {
				String sql = CommonUtils.formatJ(
						"select key_ from %s where key_ like '%s%'", tableName,
						keyPrefix);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				List<String> result = new ArrayList<String>();
				while (rs.next()) {
					result.add(rs.getString("key_"));
				}
				close(stmt);
				valueCallback.onSuccess(result);
			} catch (SQLException e) {
				valueCallback.onFailure(e);
			}
		}
	}

	class GetRangeHandler {
		public void getRange(int fromId, int toId,
				AsyncCallback<Map<Integer, String>> valueCallback) {
			try {
				String sql = CommonUtils.formatJ(
						"select id,value_ from %s where id>=%s and id<=%s",
						tableName, fromId, toId);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				Map<Integer, String> result = new LinkedHashMap<Integer, String>();
				while (rs.next()) {
					result.put(rs.getInt("id"), getValueClob(rs));
				}
				close(stmt);
				valueCallback.onSuccess(result);
			} catch (Exception e) {
				valueCallback.onFailure(e);
			}
		}
	}

	class PutHandler {
		public void put(StringMap kvs, AsyncCallback<Integer> idCallback,
				boolean add, Integer id) {
			Iterator<Entry<String, String>> kvsIterator = kvs.entrySet()
					.iterator();
			try {
				PreparedStatement stmt = null;
				while (kvsIterator.hasNext()) {
					Entry<String, String> kv = kvsIterator.next();
					if (!add) {
						if (id == null || kvs.size() != 1) {
							String sql = CommonUtils.formatJ(
									"select id from %s where key_=? ",
									tableName);
							stmt = conn.prepareStatement(sql);
							stmt.setString(1, kv.getKey());
							ResultSet rs = stmt.executeQuery();
							id = null;
							if (rs.next()) {
								id = rs.getInt(1);
							} else {
								add = true;
							}
						}
						if (id != null) {
							String sql = CommonUtils.formatJ(
									"update %s set  value_=? where id=?",
									tableName);
							stmt = conn.prepareStatement(sql);
							stmt.setCharacterStream(1,
									new StringReader(kv.getValue()));
							stmt.setInt(2, id);
							stmt.executeUpdate();
						}
					}
					if (id == null) {
						// add
						String sql = CommonUtils.formatJ(
								"insert into %s (key_,value_) values(?,?)",
								tableName);
						stmt = conn.prepareStatement(sql);
						stmt.setString(1, kv.getKey());
						// Clob clob = conn.createClob();
						// clob.setString(1, kv.getValue());
						// stmt.setClob(2, clob);
						stmt.setCharacterStream(2,
								new StringReader(kv.getValue()));
						stmt.executeUpdate();
					}
				}
				if (stmt != null) {
					stmt.close();
				}
				idCallback.onSuccess(id);
			} catch (SQLException e) {
				idCallback.onFailure(e);
			}
		}
	}

	class RemoveHandler {
		public void remove(List<String> keys, AsyncCallback<Integer> idCallback) {
			try {
				String sql = CommonUtils.formatJ(
						"select id from %s where key_ in %s ", tableName,
						LocalTransformPersistence.stringListToClause(keys));
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				List<Integer> ids = new ArrayList<Integer>();
				ids.add(-1);
				while (rs.next()) {
					ids.add(rs.getInt(1));
				}
				sql = CommonUtils.formatJ("delete from %s  where id in (%s)",
						tableName, CommonUtils.join(ids, ", "));
				stmt.executeUpdate(sql);
				close(stmt);
				idCallback.onSuccess(CommonUtils.iv(CommonUtils.first(ids)));
			} catch (SQLException e) {
				idCallback.onFailure(e);
			}
		}
	}

	class RemoveRangeHandler {
		public void removeRange(int fromId, int toId,
				AsyncCallback<Void> valueCallback) {
			try {
				String sql = CommonUtils.formatJ(
						"delete from %s where id>=? and id<=?", tableName);
				PreparedStatement stmt = conn.prepareStatement(sql);
				stmt.setInt(1, fromId);
				stmt.setInt(2, toId);
				stmt.executeUpdate();
				close(stmt);
				valueCallback.onSuccess(null);
			} catch (SQLException e) {
				valueCallback.onFailure(e);
			}
		}
	}

	public String getTableName() {
		return this.tableName;
	}
}

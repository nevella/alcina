package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.code.gwt.database.client.Database;
import com.google.code.gwt.database.client.GenericRow;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLResultSet;
import com.google.code.gwt.database.client.SQLResultSetRowList;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.code.gwt.database.client.StatementCallback;
import com.google.code.gwt.database.client.TransactionCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.client.util.DiscardInfoWrappingCallback;

public class ObjectStoreWebDbImpl implements PersistenceObjectStore {
	Database db;

	private String tableName;

	private AsyncCallback<Void> postInitCallback;

	public ObjectStoreWebDbImpl(Database db, String tableName,
			AsyncCallback<Void> postInitCallback) {
		this.db = db;
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
	public void clear(final AsyncCallback<Void> AsyncCallback) {
		TransactionCallback clearCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(AsyncCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ("Delete from %s;", tableName);
				tx.executeSql(sql, null);
			}

			@Override
			public void onTransactionSuccess() {
				AsyncCallback.onSuccess(null);
			}
		};
		db.transaction(clearCallback);
	}

	@Override
	public void drop(final AsyncCallback<Void> AsyncCallback) {
		TransactionCallback dropCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(AsyncCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ("DROP TABLE %s;", tableName);
				tx.executeSql(sql, null);
			}

			@Override
			public void onTransactionSuccess() {
				AsyncCallback.onSuccess(null);
			}
		};
		db.transaction(dropCallback);
	}

	public void executeSql(final String sql, final AsyncCallback callback) {
		final StatementCallback<GenericRow> cb = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				String message = "Problem initalising webdb - "
						+ error.getMessage() + " - " + error.getCode();
				System.out.println(message);
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
			}
		};
		TransactionCallback exCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(null, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql(sql, new String[] {}, cb);
			}

			@Override
			public void onTransactionSuccess() {
				callback.onSuccess(null);
			}
		};
		db.transaction(exCallback);
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
		TransactionCallback createCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(postInitCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ("CREATE TABLE IF NOT EXISTS "
						+ "%s" + " (id INTEGER PRIMARY KEY AUTOINCREMENT,"
						+ " key_ TEXT, value_ TEXT)  ", tableName);
				tx.executeSql(sql, null);
			}

			@Override
			public void onTransactionSuccess() {
				postInitCallback.onSuccess(null);
			}
		};
		db.transaction(createCallback);
	}

	protected void onFailure(AsyncCallback callback, SQLError error) {
		callback.onFailure(new Exception(CommonUtils.formatJ("%s: %s",
				error.getCode(), error.getMessage())));
	}

	class GetHandler {
		private StringMap getResult;

		private AsyncCallback<StringMap> valueCallback;

		private List<String> keys;

		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				ObjectStoreWebDbImpl.this.onFailure(valueCallback, error);
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				getResult = new StringMap();
				for (int i = 0; i < rs.getLength(); i++) {
					getResult.put(rs.getItem(i).getString("key_"), rs
							.getItem(i).getString("value_"));
				}
			}
		};

		TransactionCallback getCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ(
						"select key_, value_ from %s where key_ in %s",
						tableName,
						LocalTransformPersistence.stringListToClause(keys));
				tx.executeSql(sql, new String[] {}, okCallback);
			}

			@Override
			public void onTransactionSuccess() {
				valueCallback.onSuccess(getResult);
			}
		};

		public void get(List<String> keys,
				AsyncCallback<StringMap> valueCallback) {
			this.keys = keys;
			this.valueCallback = valueCallback;
			db.transaction(getCallback);
		}
	}

	class GetIdRageHandler {
		private IntPair intPair = new IntPair();

		private AsyncCallback<IntPair> valueCallback;

		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				GenericRow row = rs.getItem(0);
				intPair.i1 = row.getInt("min_");
				intPair.i2 = row.getInt("max_");
			}
		};

		TransactionCallback getCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ(
						"select ifnull(min(id),0) as min_, "
								+ "ifnull(max(id),0) as max_ from %s",
						tableName);
				tx.executeSql(sql, new String[] {}, okCallback);
			}

			@Override
			public void onTransactionSuccess() {
				valueCallback.onSuccess(intPair);
			}
		};

		public void get(AsyncCallback<IntPair> valueCallback) {
			this.valueCallback = valueCallback;
			db.transaction(getCallback);
		}
	}

	class GetPrefixedHandler {
		private List<String> getResult = new ArrayList<String>();

		private AsyncCallback<List<String>> valueCallback;

		private String keyPrefix;

		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				for (int i = 0; i < rs.getLength(); i++) {
					GenericRow row = rs.getItem(i);
					getResult.add(row.getString("key_"));
				}
				System.out.println(CommonUtils.formatJ(
						"get prefixed: [%s]\n%s\n", keyPrefix, getResult));
			}
		};

		TransactionCallback getCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ(
						"select key_ from %s where key_ like '%s%'", tableName,
						keyPrefix);
				tx.executeSql(sql, new String[] {}, okCallback);
			}

			@Override
			public void onTransactionSuccess() {
				valueCallback.onSuccess(getResult);
			}
		};

		public void get(String keyPrefix,
				AsyncCallback<List<String>> valueCallback) {
			this.keyPrefix = keyPrefix;
			this.valueCallback = valueCallback;
			db.transaction(getCallback);
		}
	}

	class GetRangeHandler {
		protected LinkedHashMap<Integer, String> getResult;

		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				getResult = new LinkedHashMap<Integer, String>();
				for (int i = 0; i < rs.getLength(); i++) {
					GenericRow row = rs.getItem(i);
					getResult.put(row.getInt("id"), row.getString("value_"));
				}
			}
		};

		TransactionCallback getCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ(
						"select id,value_ from %s where id>=? and id<=?",
						tableName);
				tx.executeSql(sql, new String[] { String.valueOf(fromId),
						String.valueOf(toId) }, okCallback);
			}

			@Override
			public void onTransactionSuccess() {
				valueCallback.onSuccess(getResult);
			}
		};

		private int fromId;

		private int toId;

		private AsyncCallback<Map<Integer, String>> valueCallback;

		public void getRange(int fromId, int toId,
				AsyncCallback<Map<Integer, String>> valueCallback) {
			this.fromId = fromId;
			this.toId = toId;
			this.valueCallback = valueCallback;
			db.transaction(getCallback);
		}
	}

	class PutHandler {
		private SQLTransaction tx;

		protected Integer id;

		StatementCallback<GenericRow> afterInsertCallback = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				if (id == null) {
					id = resultSet.getInsertId();
				}
				iterate();
			}
		};

		StatementCallback<GenericRow> getIdCallback = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				id = rs.getLength() == 0 ? null : rs.getItem(0).getInt("id");
				if (id == null) {
					add();
				} else {
					update();
				}
			}
		};

		private Entry<String, String> kv;

		TransactionCallback getCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(idCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				PutHandler.this.tx = tx;
				iterate();
			}

			@Override
			public void onTransactionSuccess() {
				idCallback.onSuccess(id);
			}
		};

		private AsyncCallback<Integer> idCallback;

		private boolean add;

		private Iterator<Entry<String, String>> kvsIterator;

		private StringMap kvs;

		public void put(StringMap kvs, AsyncCallback<Integer> idCallback,
				boolean add, Integer id) {
			this.kvs = kvs;
			this.kvsIterator = kvs.entrySet().iterator();
			this.idCallback = idCallback;
			this.add = add;
			this.id = id;
			db.transaction(getCallback);
		}

		private void update() {
			String sql = CommonUtils.formatJ(
					"update %s set  value_=? where id=?", tableName);
			tx.executeSql(sql, new String[] { kv.getValue(), id.toString() },
					afterInsertCallback);
		}

		void add() {
			String sql = CommonUtils.formatJ(
					"insert into %s (key_,value_) values(?,?)", tableName);
			tx.executeSql(sql, new String[] { kv.getKey(), kv.getValue() },
					afterInsertCallback);
		}

		void iterate() {
			if (kvsIterator.hasNext()) {
				kv = kvsIterator.next();
				if (add) {
					add();
				} else {
					if (id != null && kvs.size() == 1) {
						update();
					} else {
						String sql = CommonUtils.formatJ(
								"select id from %s where key_=? ", tableName);
						tx.executeSql(sql, new String[] { kv.getKey() },
								getIdCallback);
					}
				}
			}
		}
	}

	class RemoveHandler {
		private SQLTransaction tx;

		protected List<Integer> ids = new ArrayList<Integer>();

		StatementCallback<GenericRow> doneCallback = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
			}
		};

		StatementCallback<GenericRow> getIdCallback = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				ids.add(-1);
				for (int i = 0; i < rs.getLength(); i++) {
					ids.add(rs.getItem(i).getInt("id"));
				}
				remove();
			}
		};

		TransactionCallback getCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(idCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				RemoveHandler.this.tx = tx;
				String sql = CommonUtils.formatJ(
						"select id from %s where key_ in %s ", tableName,
						LocalTransformPersistence.stringListToClause(keys));
				tx.executeSql(sql, new String[0], getIdCallback);
			}

			@Override
			public void onTransactionSuccess() {
				idCallback.onSuccess(CommonUtils.iv(CommonUtils.first(ids)));
			}
		};

		private AsyncCallback<Integer> idCallback;

		private List<String> keys;

		public void remove(List<String> keys, AsyncCallback<Integer> idCallback) {
			this.keys = keys;
			this.idCallback = idCallback;
			db.transaction(getCallback);
		}

		private void remove() {
			String sql = CommonUtils.formatJ(
					"delete from %s  where id in (%s)", tableName,
					CommonUtils.join(ids, ", "));
			tx.executeSql(sql, new String[0], doneCallback);
		}
	}

	class RemoveRangeHandler {
		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}

			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
			}
		};

		TransactionCallback removeCallback = new TransactionCallback() {
			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}

			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ(
						"delete from %s where id>=? and id<=?", tableName);
				tx.executeSql(sql, new String[] { String.valueOf(fromId),
						String.valueOf(toId) }, okCallback);
			}

			@Override
			public void onTransactionSuccess() {
				valueCallback.onSuccess(null);
			}
		};

		private int fromId;

		private int toId;

		private AsyncCallback<Void> valueCallback;

		public void removeRange(int fromId, int toId,
				AsyncCallback<Void> valueCallback) {
			this.fromId = fromId;
			this.toId = toId;
			this.valueCallback = valueCallback;
			db.transaction(removeCallback);
		}
	}

	public String getTableName() {
		return this.tableName;
	}
}

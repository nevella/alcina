package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;

import com.google.code.gwt.database.client.Database;
import com.google.code.gwt.database.client.GenericRow;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLResultSet;
import com.google.code.gwt.database.client.SQLResultSetRowList;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.code.gwt.database.client.StatementCallback;
import com.google.code.gwt.database.client.TransactionCallback;

public class ObjectStoreWebDbImpl implements ObjectStore {
	private Database db;

	private String tableName;

	private PersistenceCallback<Void> postInitCallback;

	public ObjectStoreWebDbImpl(Database db, String tableName,
			PersistenceCallback<Void> postInitCallback) {
		this.db = db;
		this.tableName = tableName;
		this.postInitCallback = postInitCallback;
		ensureTable();
	}

	private void ensureTable() {
		TransactionCallback createCallback = new TransactionCallback() {
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

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(postInitCallback, error);
			}
		};
		db.transaction(createCallback);
	}

	@Override
	public void get(String key, PersistenceCallback<String> valueCallback) {
		new GetHandler().get(key, valueCallback);
	}

	class GetHandler {
		private String getResult;

		private PersistenceCallback<String> valueCallback;

		private String key;

		public void get(String key, PersistenceCallback<String> valueCallback) {
			this.key = key;
			this.valueCallback = valueCallback;
			db.transaction(getCallback);
		}

		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				getResult = rs.getLength() == 0 ? null : rs.getItem(0)
						.getString("value_");
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}
		};

		TransactionCallback getCallback = new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ(
						"select value_ from %s where key_=?", tableName);
				tx.executeSql(sql, new String[] { key }, okCallback);
			}

			@Override
			public void onTransactionSuccess() {
				valueCallback.onSuccess(getResult);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}
		};
	}

	class PutHandler {
		private SQLTransaction tx;

		protected Integer id;

		StatementCallback<GenericRow> afterInsertCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				if (id == null) {
					id = resultSet.getInsertId();
				}
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}
		};

		StatementCallback<GenericRow> getIdCallback = new StatementCallback<GenericRow>() {
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

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}
		};

		private void update() {
			String sql = CommonUtils.formatJ(
					"update %s set  value_=? where id=?", tableName);
			tx.executeSql(sql, new String[] { value, id.toString() },
					afterInsertCallback);
		}

		void add() {
			String sql = CommonUtils.formatJ(
					"insert into %s (key_,value_) values(?,?)", tableName);
			tx.executeSql(sql, new String[] { key, value }, afterInsertCallback);
		}

		TransactionCallback getCallback = new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				PutHandler.this.tx = tx;
				if (add) {
					add();
				} else {
					String sql = CommonUtils.formatJ(
							"select id from %s where key_=? ", tableName);
					tx.executeSql(sql, new String[] { key }, getIdCallback);
				}
			}

			@Override
			public void onTransactionSuccess() {
				idCallback.onSuccess(id);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(idCallback, error);
			}
		};

		private String key;

		private String value;

		private PersistenceCallback<Integer> idCallback;

		private boolean add;

		public void put(String key, String value,
				PersistenceCallback<Integer> idCallback, boolean add) {
			this.key = key;
			this.value = value;
			this.idCallback = idCallback;
			this.add = add;
			db.transaction(getCallback);
		}
	}

	@Override
	public void put(String key, String value,
			PersistenceCallback<Integer> idCallback) {
		new PutHandler().put(key, value, idCallback, false);
	}

	@Override
	public void add(String key, String value,
			PersistenceCallback<Integer> idCallback) {
		new PutHandler().put(key, value, idCallback, true);
	}

	class RemoveRangeHandler {
		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}
		};

		TransactionCallback removeCallback = new TransactionCallback() {
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

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}
		};

		private int fromId;

		private int toId;

		private PersistenceCallback<Void> valueCallback;

		public void removeRange(int fromId, int toId,
				PersistenceCallback<Void> valueCallback) {
			this.fromId = fromId;
			this.toId = toId;
			this.valueCallback = valueCallback;
			db.transaction(removeCallback);
		}
	}

	class GetRangeHandler {
		protected LinkedHashMap<Integer, String> getResult;

		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
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

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}
		};

		TransactionCallback getCallback = new TransactionCallback() {
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

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}
		};

		private int fromId;

		private int toId;

		private PersistenceCallback<Map<Integer, String>> valueCallback;

		public void getRange(int fromId, int toId,
				PersistenceCallback<Map<Integer, String>> valueCallback) {
			this.fromId = fromId;
			this.toId = toId;
			this.valueCallback = valueCallback;
			db.transaction(getCallback);
		}
	}

	@Override
	public void getRange(int fromId, int toId,
			PersistenceCallback<Map<Integer, String>> valueCallback) {
		new GetRangeHandler().getRange(fromId, toId, valueCallback);
	}

	protected void onFailure(PersistenceCallback callback, SQLError error) {
		callback.onFailure(new Exception(CommonUtils.formatJ("%s: %s",
				error.getCode(), error.getMessage())));
	}

	class RemoveHandler {
		private SQLTransaction tx;

		protected Integer id;

		StatementCallback<GenericRow> doneCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}
		};

		StatementCallback<GenericRow> getIdCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				id = rs.getLength() == 0 ? null : rs.getItem(0).getInt("id");
				if (id == null) {
				} else {
					remove();
				}
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}
		};

		private void remove() {
			String sql = CommonUtils.formatJ("delete from %s  where id=?",
					tableName);
			tx.executeSql(sql, new String[] { id.toString() }, doneCallback);
		}

		TransactionCallback getCallback = new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				RemoveHandler.this.tx = tx;
				String sql = CommonUtils.formatJ(
						"select id from %s where key_=? ", tableName);
				tx.executeSql(sql, new String[] { key }, getIdCallback);
			}

			@Override
			public void onTransactionSuccess() {
				idCallback.onSuccess(id);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(idCallback, error);
			}
		};

		private String key;

		private PersistenceCallback<Integer> idCallback;

		public void remove(String key, PersistenceCallback<Integer> idCallback) {
			this.key = key;
			this.idCallback = idCallback;
			db.transaction(getCallback);
		}
	}

	class GetPrefixedHandler {
		private List<String> getResult = new ArrayList<String>();

		private PersistenceCallback<List<String>> valueCallback;

		private String keyPrefix;

		public void get(String keyPrefix,
				PersistenceCallback<List<String>> valueCallback) {
			this.keyPrefix = keyPrefix;
			this.valueCallback = valueCallback;
			db.transaction(getCallback);
		}

		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
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

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}
		};

		TransactionCallback getCallback = new TransactionCallback() {
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

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}
		};
	}

	class GetIdRageHandler {
		private IntPair intPair = new IntPair();

		private PersistenceCallback<IntPair> valueCallback;

		public void get(PersistenceCallback<IntPair> valueCallback) {
			this.valueCallback = valueCallback;
			db.transaction(getCallback);
		}

		StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				GenericRow row = rs.getItem(0);
				intPair.i1 = row.getInt("min_");
				intPair.i2 = row.getInt("max_");
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				return true;
			}
		};

		TransactionCallback getCallback = new TransactionCallback() {
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

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(valueCallback, error);
			}
		};
	}

	public void executeSql(final String sql, final PersistenceCallback callback) {
		final StatementCallback<GenericRow> cb = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				String message = "Problem initalising webdb - "
						+ error.getMessage() + " - " + error.getCode();
				System.out.println(message);
				return true;
			}
		};
		TransactionCallback exCallback = new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql(sql, new String[] {}, cb);
			}

			@Override
			public void onTransactionSuccess() {
				callback.onSuccess(null);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(null, error);
			}
		};
		db.transaction(exCallback);
	}

	@Override
	public void remove(String key, PersistenceCallback<Integer> idCallback) {
		new RemoveHandler().remove(key, idCallback);
	}

	@Override
	public void getKeysPrefixedBy(String keyPrefix,
			PersistenceCallback<List<String>> completedCallback) {
		new GetPrefixedHandler().get(keyPrefix, completedCallback);
	}

	@Override
	public void getIdRange(PersistenceCallback<IntPair> completedCallback) {
		new GetIdRageHandler().get(completedCallback);
	}

	@Override
	public void removeIdRange(IntPair range,
			PersistenceCallback<Void> completedCallback) {
		new RemoveRangeHandler().removeRange(range.i1, range.i2,
				completedCallback);
	}

	@Override
	public void drop(final PersistenceCallback<Void> persistenceCallback) {
		TransactionCallback dropCallback = new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				String sql = CommonUtils.formatJ("DROP TABLE %s;", tableName);
				tx.executeSql(sql, null);
			}

			@Override
			public void onTransactionSuccess() {
				persistenceCallback.onSuccess(null);
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				onFailure(persistenceCallback, error);
			}
		};
		db.transaction(dropCallback);
	}
}

package cc.alcina.framework.gwt.persistence.client;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.code.gwt.database.client.Database;
import com.google.code.gwt.database.client.GenericRow;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLResultSet;
import com.google.code.gwt.database.client.SQLResultSetRowList;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.code.gwt.database.client.StatementCallback;
import com.google.code.gwt.database.client.TransactionCallback;

public class ObjectStoreWebDbImpl {
	private final Database db;

	private final String tableName;

	private final PersistenceCallback<Void> postInitCallback;

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
						+ " key TEXT, value TEXT)  ", tableName);
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

	public void get(final String key,
			final PersistenceCallback<String> valueCallback) {
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

		final StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				getResult = rs.getLength() == 0 ? null : rs.getItem(0)
						.getString("value");
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
						"select value from %s where key=?", tableName);
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
				id = resultSet.getInsertId();
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
					"update %s set key=? and value=? where id=?", tableName);
			tx.executeSql(sql, new String[] { key, value, id.toString() },
					afterInsertCallback);
		}

		void add() {
			String sql = CommonUtils.formatJ(
					"insert into %s (key,value) values(?,?)", tableName);
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
							"select id from %s where key=? ", tableName);
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

	public void put(String key, String value,
			PersistenceCallback<Integer> idCallback) {
		new PutHandler().put(key, value, idCallback, false);
	}

	public void add(String key, String value,
			PersistenceCallback<Integer> idCallback) {
		new PutHandler().put(key, value, idCallback, true);
	}

	class GetRangeHandler {
		protected LinkedHashMap<Integer, String> getResult;

		final StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				getResult = new LinkedHashMap<Integer, String>();
				for (int i = 0; i < rs.getLength(); i++) {
					GenericRow row = rs.getItem(i);
					getResult.put(row.getInt("id"), row.getString("value"));
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
						"select id,value from %s where id>=? and id<=?",
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

	public void getRange(final int fromId, final int toId,
			final PersistenceCallback<Map<Integer, String>> valueCallback) {
	}

	protected void onFailure(PersistenceCallback callback, SQLError error) {
		callback.onFailure(new Exception(CommonUtils.formatJ("%s: %s",
				error.getCode(), error.getMessage())));
	}
}

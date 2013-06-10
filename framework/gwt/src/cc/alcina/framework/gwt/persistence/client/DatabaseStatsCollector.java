package cc.alcina.framework.gwt.persistence.client;

import java.util.Map.Entry;
import java.util.Set;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CountingMap;

import com.google.code.gwt.database.client.Database;
import com.google.code.gwt.database.client.GenericRow;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLResultSet;
import com.google.code.gwt.database.client.SQLResultSetRowList;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.code.gwt.database.client.StatementCallback;
import com.google.code.gwt.database.client.TransactionCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class DatabaseStatsCollector {
	public static class DatabaseStatsInfo {
		CountingMap<String> transformTexts = new CountingMap<String>();

		CountingMap<String> transformCounts = new CountingMap<String>();

		CountingMap<Integer> logSizes = new CountingMap<Integer>();

		@Override
		public String toString() {
			String out = "\n\nDatabase stats:\n========\n\nTransforms: \n";
			Set<Entry<String, Integer>> entrySet = transformTexts.entrySet();
			String template = "\t%s : %s  -  %s chars\n";
			for (Entry<String, Integer> entry : entrySet) {
				out += CommonUtils.formatJ(template,
						CommonUtils.padStringRight(entry.getKey(), 20, ' '),
						transformCounts.get(entry.getKey()), entry.getValue());
			}
			out += CommonUtils.formatJ(template,
					CommonUtils.padStringRight("total", 20, ' '),
					transformCounts.sum(), transformTexts.sum());
			out += "\nLogs: \n";
			out += CommonUtils.formatJ(template,
					CommonUtils.padStringRight("total", 20, ' '),
					logSizes.size(), logSizes.sum());
			return out;
		}
	}

	private DatabaseStatsCollector.Phase phase = Phase.TRANSFORMS_DB_QUERY;

	enum Phase {
		TRANSFORMS_DB_QUERY, LOGS_DB, FINISHED
	}

	DatabaseStatsCollector.DatabaseStatsInfo info = new DatabaseStatsInfo();

	private AsyncCallback<DatabaseStatsInfo> infoCallback;

	private String logTableName;

	public void run(String logTableName,
			AsyncCallback<DatabaseStatsInfo> infoCallback) {
		this.logTableName = logTableName;
		this.infoCallback = infoCallback;
		iterate();
	}

	void iterate() {
		switch (phase) {
		case TRANSFORMS_DB_QUERY:
			statTransforms();
			break;
		case LOGS_DB:
			statLogs();
			break;
		case FINISHED:
			infoCallback.onSuccess(info);
			break;
		}
	}

	private void statTransforms() {
		Database db = ((WebDatabaseTransformPersistence) LocalTransformPersistence
				.get()).getDb();
		final StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				for (int i = 0; i < rs.getLength(); i++) {
					GenericRow row = rs.getItem(i);
					String key = row.getString("transform_request_type");
					int size = row.getString("transform").length();
					info.transformCounts.add(key);
					info.transformTexts.add(key, size);
				}
				phase = Phase.LOGS_DB;
				iterate();
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				fail(error);
				return true;
			}
		};
		db.transaction(new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql("select * from TransformRequests ", null,
						okCallback);
			}

			@Override
			public void onTransactionSuccess() {
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				fail(error);
			}
		});
	}

	private void statLogs() {
		Database db = ((ObjectStoreWebDbImpl) LogStore.get().objectStore).db;
		final StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				for (int i = 0; i < rs.getLength(); i++) {
					GenericRow row = rs.getItem(i);
					int key = row.getInt("id");
					int size = row.getString("value_").length();
					info.logSizes.add(key, size);
				}
				phase = Phase.FINISHED;
				iterate();
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				fail(error);
				return true;
			}
		};
		db.transaction(new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql(
						CommonUtils.formatJ("select * from %s ", logTableName),
						null, okCallback);
			}

			@Override
			public void onTransactionSuccess() {
			}

			@Override
			public void onTransactionFailure(SQLError error) {
				fail(error);
			}
		});
	}

	protected void fail(SQLError error) {
		infoCallback
				.onFailure(new Exception("SQLError: " + error.getMessage()));
	}
}
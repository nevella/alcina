package cc.alcina.framework.gwt.persistence.client;

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
import com.google.gwt.user.client.rpc.AsyncCallback;

public class DatabaseStatsCollector {
	private DatabaseStatsCollector.Phase phase = Phase.TRANSFORMS_DB_QUERY;

	enum Phase {
		TRANSFORMS_DB_QUERY, LOGS_DB, FINISHED
	}

	DatabaseStatsInfo info = new DatabaseStatsInfo();

	private AsyncCallback<DatabaseStatsInfo> infoCallback;

	private String logTableName;

	private long start;

	public void run(AsyncCallback<DatabaseStatsInfo> infoCallback) {
		this.logTableName = LogStore.DEFAULT_TABLE_NAME;
		this.infoCallback = infoCallback;
		start = System.currentTimeMillis();
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
			info.setCollectionTimeMs(System.currentTimeMillis() - start);
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
					info.getTransformCounts().add(key);
					info.getTransformTexts().add(key, size);
					if (key.equals(DomainTransformRequestType.CLIENT_OBJECT_LOAD
							.toString())) {
						info.getClientObjectLoadSizes().add(size);
					}
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
				tx.executeSql("select * from TransformRequests order by id",
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
					info.getLogSizes().add(key, size);
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
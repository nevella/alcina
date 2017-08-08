package cc.alcina.framework.gwt.persistence.client;

import com.google.code.gwt.database.client.Database;
import com.google.code.gwt.database.client.GenericRow;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLResultSet;
import com.google.code.gwt.database.client.SQLResultSetRowList;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.code.gwt.database.client.StatementCallback;
import com.google.code.gwt.database.client.TransactionCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaSignature;
import cc.alcina.framework.common.client.util.CommonUtils;

public class DatabaseStatsCollector {
	private DatabaseStatsCollector.Phase phase = Phase.TRANSFORMS_DB_QUERY;

	enum Phase {
		TRANSFORMS_DB_QUERY, LOGS_DB, DELTAS_DB, FINISHED
	}

	DatabaseStatsInfo info = new DatabaseStatsInfo();

	private AsyncCallback<DatabaseStatsInfo> infoCallback;

	private long start;

	public void run(AsyncCallback<DatabaseStatsInfo> infoCallback) {
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
			ObjectStoreWebDbImpl logWebDbStore = (ObjectStoreWebDbImpl) LogStore
					.get().objectStore;
			statStore(logWebDbStore, info, true, Phase.DELTAS_DB);
			break;
		case DELTAS_DB:
			ObjectStoreWebDbImpl deltaWebDbStore = (ObjectStoreWebDbImpl) DeltaStore
					.get().objectStore;
			statStore(deltaWebDbStore, info, false, Phase.FINISHED);
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

	private void statStore(final ObjectStoreWebDbImpl dbStore,
			final DatabaseStatsInfo info, final boolean logs,
			final Phase nextPhase) {
		final StatementCallback<GenericRow> okCallback = new StatementCallback<GenericRow>() {
			@Override
			public void onSuccess(SQLTransaction transaction,
					SQLResultSet<GenericRow> resultSet) {
				SQLResultSetRowList<GenericRow> rs = resultSet.getRows();
				for (int i = 0; i < rs.getLength(); i++) {
					GenericRow row = rs.getItem(i);
					int id = row.getInt("id");
					String key = row.getString("key_");
					int size = row.getString("value_").length();
					if (logs) {
						info.getLogSizes().add(id, size);
					} else {
						DomainModelDeltaSignature sig = DeltaStore
								.parseSignature(key);
						String nvk = sig.nonVersionedSignature();
						if (key.startsWith(DeltaStore.META)) {
							info.getDeltaCounts().add(nvk);
						}
						info.getDeltaSizes().add(nvk, size);
					}
				}
				phase = nextPhase;
				iterate();
			}

			@Override
			public boolean onFailure(SQLTransaction transaction, SQLError error) {
				fail(error);
				return true;
			}
		};
		dbStore.db.transaction(new TransactionCallback() {
			@Override
			public void onTransactionStart(SQLTransaction tx) {
				tx.executeSql(
						CommonUtils.formatJ("select * from %s ",
								dbStore.getTableName()), null, okCallback);
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
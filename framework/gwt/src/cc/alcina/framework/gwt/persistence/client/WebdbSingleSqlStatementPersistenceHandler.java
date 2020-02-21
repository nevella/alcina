package cc.alcina.framework.gwt.persistence.client;

import com.google.code.gwt.database.client.GenericRow;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.code.gwt.database.client.StatementCallback;
import com.google.code.gwt.database.client.TransactionCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.util.Ax;

public abstract class WebdbSingleSqlStatementPersistenceHandler<T>
		implements StatementCallback<GenericRow>, TransactionCallback {
	protected String sql;

	protected AsyncCallback<T> postTransactionCallback;

	protected Object[] arguments;

	protected SQLError statementError;

	public WebdbSingleSqlStatementPersistenceHandler(String sql,
			AsyncCallback<T> postTransactionCallback) {
		this(sql, postTransactionCallback, (Object[]) null);
	}

	public WebdbSingleSqlStatementPersistenceHandler(String sql,
			AsyncCallback<T> postTransactionCallback, Object... arguments) {
		this.sql = sql;
		this.postTransactionCallback = postTransactionCallback;
		this.arguments = arguments;
	}

	@Override
	public boolean onFailure(SQLTransaction transaction, SQLError error) {
		statementError = error;
		return true;
	}

	public void onTransactionFailure(SQLError error) {
		postTransactionCallback
				.onFailure(new Exception(Ax.format("%s :: %s",
						(statementError == null ? "<no statement error>"
								: statementError.getMessage()),
						error.getMessage())));
	}

	public void onTransactionStart(SQLTransaction tx) {
		tx.executeSql(sql, arguments, this);
	}

	public void onTransactionSuccess() {
		postTransactionCallback.onSuccess(getResult());
	}

	protected abstract T getResult();
}
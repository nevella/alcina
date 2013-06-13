package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.code.gwt.database.client.GenericRow;
import com.google.code.gwt.database.client.SQLError;
import com.google.code.gwt.database.client.SQLTransaction;
import com.google.code.gwt.database.client.StatementCallback;
import com.google.code.gwt.database.client.TransactionCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class WebdbSingleSqlStatementPersistenceHandler<T> implements
		StatementCallback<GenericRow>, TransactionCallback {
	protected String sql;

	protected AsyncCallback<T> postTransactionCallback;

	protected Object[] arguments;

	protected SQLError statementError;

	protected abstract T getResult();

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

	public void onTransactionStart(SQLTransaction tx) {
		tx.executeSql(sql, arguments, this);
	}

	public void onTransactionFailure(SQLError error) {
		postTransactionCallback.onFailure(new Exception(CommonUtils.formatJ(
				"%s :: %s", (statementError == null ? "<no statement error>"
						: statementError.getMessage()), error.getMessage())));
	}

	public void onTransactionSuccess() {
		postTransactionCallback.onSuccess(getResult());
	}

	@Override
	public boolean onFailure(SQLTransaction transaction, SQLError error) {
		statementError = error;
		return true;
	}
}
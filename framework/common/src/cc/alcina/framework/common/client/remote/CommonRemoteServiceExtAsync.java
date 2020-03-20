package cc.alcina.framework.common.client.remote;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.search.SearchDefinition;

public interface CommonRemoteServiceExtAsync extends CommonRemoteServiceAsync {
	public void getLogsForAction(RemoteAction action, Integer count,
			AsyncCallback<List<ActionLogItem>> callback);

	public void performAction(RemoteAction action,
			AsyncCallback<String> callback);

	public void performActionAndWait(RemoteAction action,
			AsyncCallback<ActionLogItem> callback);

	public void search(SearchDefinition def, int pageNumber,
			AsyncCallback<SearchResultsBase> callback);

	<G extends WrapperPersistable> void persist(G gwpo,
			AsyncCallback<Long> callback);

	void log(ILogRecord remoteLogRecord, AsyncCallback<Long> callback);
}

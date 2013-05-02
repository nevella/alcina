package cc.alcina.framework.common.client.remote;

import java.util.List;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.search.SearchDefinition;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface CommonRemoteServiceExtAsync extends CommonRemoteServiceAsync{

	public void getLogsForAction(RemoteAction action, Integer count,
	AsyncCallback<List<ActionLogItem>> callback);

	public void performAction(RemoteAction action, AsyncCallback<Long> callback);

	public void performActionAndWait(RemoteAction action,
	AsyncCallback<ActionLogItem> callback);

	public void search(SearchDefinition def, int pageNumber,
	AsyncCallback<SearchResultsBase> callback);

	<G extends WrapperPersistable> void persist(G gwpo,
	AsyncCallback<Long> callback);
	}

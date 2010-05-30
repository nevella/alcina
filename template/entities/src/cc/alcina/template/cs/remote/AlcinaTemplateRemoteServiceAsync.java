package cc.alcina.template.cs.remote;

import java.util.List;

import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface AlcinaTemplateRemoteServiceAsync extends CommonRemoteServiceAsync {
	public void loadInitial(AsyncCallback callback);

	public void search(SearchDefinition def, int pageNumber,
			AsyncCallback callback);


	public void getAllGroups(AsyncCallback<List<AlcinaTemplateGroup>> callback);
}

package cc.alcina.template.cs.remote;

import java.util.List;

import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.csobjects.LoadObjectsResponse;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceExt;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("alcinaTemplateService")
public interface AlcinaTemplateRemoteService extends CommonRemoteServiceExt {
	public LoadObjectsResponse loadInitial(LoadObjectsRequest request);

	public List<AlcinaTemplateGroup> getAllGroups();

}

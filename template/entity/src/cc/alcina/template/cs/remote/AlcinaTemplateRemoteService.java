package cc.alcina.template.cs.remote;

import java.util.List;

import cc.alcina.framework.common.client.remote.CommonRemoteServiceExt;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("alcinaTemplateService")
public interface AlcinaTemplateRemoteService extends CommonRemoteServiceExt {
	public AlcinaTemplateObjects loadInitial();

	public List<AlcinaTemplateGroup> getAllGroups();

}

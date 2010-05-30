package cc.alcina.template.entityaccess;

import java.util.List;

import cc.alcina.framework.common.client.logic.template.AlcinaTemplate;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.cs.persistent.AlcinaTemplateGroup;

@AlcinaTemplate
public interface AlcinaTemplatePersistenceLocal {

	public List<AlcinaTemplateGroup> getVisibleGroups();
	public void init() throws Exception;
	public void destroy();
	public AlcinaTemplateObjects loadInitial(boolean internal) throws Exception;
	public List<AlcinaTemplateGroup> getAllGroups();
}
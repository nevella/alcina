package cc.alcina.framework.common.client.remote;

import java.util.List;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.WebMethod;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.search.SearchDefinition;

public interface CommonRemoteServiceExt extends CommonRemoteService{

	@WebMethod(customPermission = @Permission(access = AccessLevel.ADMIN))
	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count);

	@WebMethod()
	public Long performAction(RemoteAction action);

	@WebMethod()
	public ActionLogItem performActionAndWait(RemoteAction action)
			throws WebException;

	public SearchResultsBase search(SearchDefinition def, int pageNumber);

	@WebMethod
	public <G extends WrapperPersistable> Long persist(G gwpo)
			throws WebException;
}

package cc.alcina.framework.common.client.remote;

import java.util.List;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadRequest;
import cc.alcina.framework.common.client.logic.domaintransform.PartialDtrUploadResponse;
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

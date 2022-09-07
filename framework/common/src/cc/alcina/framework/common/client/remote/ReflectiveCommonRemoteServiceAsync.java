package cc.alcina.framework.common.client.remote;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.log.ILogRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.search.SearchDefinition;

@Reflected
@Registration.Singleton
public class ReflectiveCommonRemoteServiceAsync extends
		ReflectiveRemoteServiceAsync implements CommonRemoteServiceAsync {
	public static ReflectiveCommonRemoteServiceAsync get() {
		return Registry.impl(ReflectiveCommonRemoteServiceAsync.class);
	}

	@Override
	public void getJobLog(long jobId, AsyncCallback<String> callback) {
		call("getJobLog", new Class[] { long.class }, callback, jobId);
	}

	@Override
	public void getLogsForAction(RemoteAction action, Integer count,
			AsyncCallback<List<ActionLogItem>> callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void hello(AsyncCallback callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void listRunningJobs(AsyncCallback<List<String>> callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void log(ILogRecord remoteLogRecord, AsyncCallback<Long> callback) {
		call("log", new Class[] { ILogRecord.class }, callback,
				remoteLogRecord);
	}

	@Override
	public void logClientError(String exceptionToString,
			AsyncCallback<Long> callback) {
		call("logClientError", new Class[] { String.class }, callback,
				exceptionToString);
	}

	@Override
	public void logClientError(String exceptionToString, String exceptionType,
			AsyncCallback<Long> callback) {
		call("logClientError", new Class[] { String.class, String.class },
				callback, exceptionToString, exceptionType);
	}

	@Override
	public void logClientRecords(String serializedLogRecords,
			AsyncCallback<Void> callback) {
		call("logClientRecords", new Class[] { String.class }, callback,
				serializedLogRecords);
	}

	@Override
	public void login(LoginBean loginBean, AsyncCallback callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void logout(AsyncCallback callback) {
		call("logout", new Class[] {}, callback);
	}

	@Override
	public void performAction(RemoteAction action,
			AsyncCallback<String> callback) {
		call("performAction", new Class[] { RemoteAction.class }, callback,
				action);
	}

	@Override
	public void persistOfflineTransforms(
			List<DeltaApplicationRecord> uncommitted,
			AsyncCallback<Void> callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void ping(AsyncCallback<Void> callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void pollJobStatus(JobTracker.Request request,
			AsyncCallback<JobTracker.Response> callback) {
		call("pollJobStatus", new Class[] { JobTracker.Request.class },
				callback, request);
	}

	@Override
	public void pollJobStatus(String id, boolean cancel,
			AsyncCallback<JobTracker> callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void search(SearchDefinition def,
			AsyncCallback<SearchResultsBase> callback) {
		throw new UnsupportedOperationException();
	}

	public void submitPublication(
			ContentRequestBase<? extends ContentDefinition> publicationRequest,
			AsyncCallback<EntityLocator> callback) {
		call("submitPublication", new Class[] { ContentRequestBase.class },
				callback, publicationRequest);
	}

	@Override
	public void transform(DomainTransformRequest request,
			AsyncCallback<DomainTransformResponse> callback) {
		call("transform", new Class[] { DomainTransformRequest.class },
				callback, request);
	}

	@Override
	public void validateOnServer(List<ServerValidator> validators,
			AsyncCallback<List<ServerValidator>> callback) {
		call("validateOnServer", new Class[] { List.class }, callback,
				validators);
	}

	@Override
	public void waitForTransforms(DomainTransformCommitPosition position,
			AsyncCallback<DomainUpdate> callback) {
		throw new UnsupportedOperationException();
	}
}

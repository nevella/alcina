/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.servlet.servlet;

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.AuthenticationRequired;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.remote.CommonRemoteService;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.actions.RequiresHttpRequest;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.domaintransform.HiliLocatorMap;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.domaintransform.TransformPersistenceToken;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformRequestPersistence.DomainTransformRequestPersistenceEvent;
import cc.alcina.framework.entity.domaintransform.event.DomainTransformRequestPersistence.DomainTransformRequestPersistenceSupport;
import cc.alcina.framework.entity.domaintransform.policy.IgnoreMissingPersistenceLayerTransformExceptionPolicy;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.servlet.CookieHelper;
import cc.alcina.framework.servlet.ServletLayerLocator;
import cc.alcina.framework.servlet.ServletLayerRegistry;
import cc.alcina.framework.servlet.SessionHelper;
import cc.alcina.framework.servlet.authentication.AuthenticationException;
import cc.alcina.framework.servlet.job.JobRegistry;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.google.gwt.user.server.rpc.impl.LegacySerializationPolicy;

/**
 * Tests (todo) for transform persistence: invalid clientauth multiple
 * simultaneous (identical clientinstance, non-) cross-server-restart
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
public abstract class CommonRemoteServiceServlet extends RemoteServiceServlet
		implements CommonRemoteService {
	/**
	 * ibid
	 */
	private Logger logger;

	public static final String THRD_LOCAL_RPC_RQ = "THRD_LOCAL_RPC_RQ";

	private int actionCount = 0;

	public List<ObjectCacheItemResult> cache(List<ObjectCacheItemSpec> specs)
			throws WebException {
		try {
			return ServletLayerLocator.get().commonPersistenceProvider()
					.getCommonPersistence().cache(specs);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebException(e);
		}
	}

	public <T extends HasIdAndLocalId> T getItemById(String className, Long id)
			throws WebException {
		try {
			Class<T> clazz = (Class<T>) Class.forName(className);
			return ServletLayerLocator.get().commonPersistenceProvider()
					.getCommonPersistence().getItemById(clazz, id, true, false);
		} catch (Exception e) {
			throw new WebException(e.getMessage());
		}
	}

	public Logger getLogger() {
		return logger;
	}

	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count) {
		checkAnnotatedPermissions(action);
		return ServletLayerLocator.get().commonPersistenceProvider()
				.getCommonPersistence().listLogItemsForClass(
						action.getClass().getName(), count);
	}

	public List<Long> listRunningJobs() {
		return JobRegistry.get().getRunningJobs();
	}

	public Long logClientError(String exceptionToString) {
		return logClientError(exceptionToString,
				LogMessageType.CLIENT_EXCEPTION.toString());
	}

	public Long logClientError(String exceptionToString, String exceptionType) {
		return ServletLayerLocator.get().commonPersistenceProvider()
				.getCommonPersistence().log(exceptionToString, exceptionType);
	}

	public void logRpcException(Exception ex) {
		RPCRequest rpcRequest = getThreadLocalRequest() == null ? null
				: (RPCRequest) getThreadLocalRequest().getAttribute(
						THRD_LOCAL_RPC_RQ);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String msg = "RPC exception:\n";
		if (rpcRequest != null) {
			msg += "Method: " + rpcRequest.getMethod().getName() + "\n";
			msg += "Parameters: \n";
			Object[] parameters = rpcRequest.getParameters();
			int i = 0;
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			XMLEncoder enc = new XMLEncoder(os);
			for (Object object : parameters) {
				String xml = "";
				if (object != null
						&& CommonUtils.isStandardJavaClass(object.getClass())) {
					try {
						enc.writeObject(object);
						enc.flush();
						xml = new String(os.toByteArray());
						os.reset();
					} catch (Exception e) {
						xml = "Unable to write object - "
								+ object.getClass().getName();
					}
				}
				msg += CommonUtils.format("\t [%1] - %2\n\t   - %3\n", i++,
						object, xml);
			}
		}
		msg += "Stacktrace:\t " + sw.toString();
		CommonPersistenceLocal cpl = ServletLayerLocator.get()
				.commonPersistenceProvider().getCommonPersistence();
		cpl.log(msg, LogMessageType.RPC_EXCEPTION.toString());
	}

	public Long performAction(final RemoteAction action) {
		return performAction(action, true);
	}

	public Long performAction(final RemoteAction action,
			final boolean persistentLog) {
		checkAnnotatedPermissions(action);
		final RemoteActionPerformer performer = (RemoteActionPerformer) ServletLayerRegistry
				.get().instantiateSingle(RemoteActionPerformer.class,
						action.getClass());
		final PermissionsManager pm = PermissionsManager.get();
		Thread thread = new Thread(performer.getClass().getSimpleName() + " - "
				+ (++actionCount)) {
			@Override
			public void run() {
				// different thread-local
				pm.copyTo(PermissionsManager.get());
				ActionLogItem result = null;
				result = performer.performAction(action);
				result.setActionClass(action.getClass());
				result.setActionDate(new Date());
				if (persistentLog) {
					ServletLayerLocator.get().commonPersistenceProvider()
							.getCommonPersistence().logActionItem(result);
				}
			}
		};
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
		return thread.getId();
	}

	public ActionLogItem performActionAndWait(final RemoteAction action)
			throws WebException {
		checkAnnotatedPermissions(action);
		RemoteActionPerformer performer = (RemoteActionPerformer) ServletLayerRegistry
				.get().instantiateSingle(RemoteActionPerformer.class,
						action.getClass());
		ActionLogItem result = null;
		if (performer instanceof RequiresHttpRequest) {
			RequiresHttpRequest req = (RequiresHttpRequest) performer;
			req.setHttpServletRequest(getThreadLocalRequest());
		}
		try {
			result = performer.performAction(action);
			if (result != null) {
				result.setActionClass(action.getClass());
				result.setActionDate(new Date());
				ServletLayerLocator.get().commonPersistenceProvider()
						.getCommonPersistence().logActionItem(result);
			}
			return result;
		} catch (Exception e) {
			boolean log = true;
			if (e instanceof WrappedRuntimeException) {
				WrappedRuntimeException ire = (WrappedRuntimeException) e;
				log = ire.getSuggestedAction() != SuggestedAction.EXPECTED_EXCEPTION;
			}
			if (log) {
				logRpcException(e);
			}
			throw new WebException(e);
		}
	}

	public <G extends WrapperPersistable> Long persist(G gwpo)
			throws WebException {
		try {
			return ServletLayerLocator.get().commonPersistenceProvider()
					.getCommonPersistence().persist(gwpo);
		} catch (Exception e) {
			logger.warn(e);
			throw new WebException(e.getMessage());
		}
	}

	// TODO - well, lock sync
	public void persistOfflineTransforms(
			List<DTRSimpleSerialWrapper> uncommitted) throws WebException {
		persistOfflineTransforms(uncommitted, null);
	}

	public void persistOfflineTransforms(
			List<DTRSimpleSerialWrapper> uncommitted, Logger logger)
			throws WebException {
		CommonPersistenceLocal cp = ServletLayerLocator.get()
				.commonPersistenceProvider().getCommonPersistence();
		try {
			Class<? extends ClientInstance> clientInstanceClass = cp
					.getImplementation(ClientInstance.class);
			Class<? extends DomainTransformRequestPersistent> dtrClass = cp
					.getImplementation(DomainTransformRequestPersistent.class);
			long currentClientInstanceId = 0;
			for (DTRSimpleSerialWrapper wr : uncommitted) {
				long clientInstanceId = wr.getClientInstanceId();
				int requestId = (int) wr.getRequestId();
				DomainTransformRequest alreadyWritten = cp
						.getItemByKeyValueKeyValue(dtrClass,
								"clientInstance.id", clientInstanceId,
								"requestId", requestId);
				if (alreadyWritten != null) {
					if (logger != null) {
						logger.warn(CommonUtils.format(
								"Request [%1/%2] already written", requestId,
								clientInstanceId));
					}
					continue;
				}
				DomainTransformRequest rq = new DomainTransformRequest();
				rq
						.setDomainTransformRequestType(DomainTransformRequestType.CLIENT_STARTUP_FROM_OFFLINE);
				ClientInstance clientInstance = clientInstanceClass
						.newInstance();
				clientInstance.setAuth(wr.getClientInstanceAuth());
				clientInstance.setId(wr.getClientInstanceId());
				rq.setClientInstance(clientInstance);
				rq.getClientInstance().setUser(
						PermissionsManager.get().getUser());
				// TODO - perhaps allow facility to persist multi-user
				// transforms. but...perhaps better not (keep as is)
				rq.setRequestId(wr.getRequestId());
				rq.fromString(wr.getText());
				DomainTransformLayerWrapper transformLayerWrapper = transform(
						rq,
						true,
						new IgnoreMissingPersistenceLayerTransformExceptionPolicy());
				if (logger != null) {
					logger
							.info(CommonUtils
									.format(
											"Request [%1/%2] : %3 transforms written, %4 ignored",
											requestId, clientInstanceId, rq
													.getItems().size(),
											transformLayerWrapper.ignored));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebException(e);
		}
	}

	public JobInfo pollJobStatus(Long id, boolean cancel) {
		if (cancel) {
			JobRegistry.get().cancel(id);
		}
		return JobRegistry.get().getInfo(id);
	}

	@Override
	public String processCall(String payload) throws SerializationException {
		RPCRequest rpcRequest = null;
		try {
			CookieHelper.get().getIid(getThreadLocalRequest(),
					getThreadLocalResponse());
			SessionHelper.initUserState(getThreadLocalRequest());
			String userName = CookieHelper.get().getRememberedUserName(
					getThreadLocalRequest(), getThreadLocalResponse());
			if (userName != null && !PermissionsManager.get().isLoggedIn()) {
				try {
					LoginResponse lrb = new LoginResponse();
					lrb.setOk(true);
					processValidLogin(lrb, userName);
				} catch (AuthenticationException e) {
					// ignore
				}
			}
			rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
			if (rpcRequest.getSerializationPolicy() instanceof LegacySerializationPolicy) {
				throw new IncompatibleRemoteServiceException();
			}
			getThreadLocalRequest().setAttribute(THRD_LOCAL_RPC_RQ, rpcRequest);
			String name = rpcRequest.getMethod().getName();
			Method method;
			try {
				method = this.getClass().getMethod(name,
						rpcRequest.getMethod().getParameterTypes());
				if (method.isAnnotationPresent(AuthenticationRequired.class)) {
					AuthenticationRequired ar = method
							.getAnnotation(AuthenticationRequired.class);
					AnnotatedPermissible ap = new AnnotatedPermissible(ar
							.permission());
					if (!PermissionsManager.get().isPermissible(ap)) {
						getServletContext().log(
								"Action not permitted: " + name,
								new Exception());
						return RPC.encodeResponseForFailure(null,
								new WebException("Action not permitted: "
										+ name));
					}
				}
			} catch (SecurityException ex) {
				RPC.encodeResponseForFailure(null, ex);
			} catch (NoSuchMethodException ex) {
				RPC.encodeResponseForFailure(null, ex);
			}
			return RPC.invokeAndEncodeResponse(this, rpcRequest.getMethod(),
					rpcRequest.getParameters(), rpcRequest
							.getSerializationPolicy());
		} catch (IncompatibleRemoteServiceException ex) {
			getServletContext()
					.log(
							"An IncompatibleRemoteServiceException was thrown while processing this call.",
							ex);
			return RPC.encodeResponseForFailure(null, ex);
		} catch (UnexpectedException ex) {
			logRpcException(ex);
			throw ex;
		} finally {
			ThreadlocalTransformManager.cast().resetTltm(null);
		}
	}

	public SearchResultsBase search(SearchDefinition def, int pageNumber) {
		return ServletLayerLocator.get().commonPersistenceProvider()
				.getCommonPersistence().search(def, pageNumber);
	}

	public DomainTransformResponse transform(DomainTransformRequest request)
			throws DomainTransformRequestException {
		return transform(request, false, null).response;
	}

	public DomainTransformResponse transformFromServletLayer(
			boolean persistTransforms) throws DomainTransformRequestException {
		DomainTransformLayerWrapper wrapper = transformFromServletLayer(
				persistTransforms, null);
		return wrapper == null ? null : wrapper.response;
	}

	/*
	 * TODO - this should probably be integrated more with {transform} - why is
	 * the server layer so special? just another client
	 */
	public DomainTransformLayerWrapper transformFromServletLayer(
			boolean persistTransforms,
			PersistenceLayerTransformExceptionPolicy transformExceptionPolicy)
			throws DomainTransformRequestException {
		DomainTransformRequest request = new DomainTransformRequest();
		HiliLocatorMap map = new HiliLocatorMap();
		request.setClientInstance(CommonRemoteServiceServletSupport.get()
				.getServerAsClientInstance());
		request.setRequestId(nextTransformRequestId());
		ArrayList<DomainTransformEvent> items = new ArrayList<DomainTransformEvent>(
				TransformManager.get().getTransformsByCommitType(
						CommitType.TO_LOCAL_BEAN));
		if (items.isEmpty()) {
			return null;
		}
		for (DomainTransformEvent dte : items) {
			dte.setCommitType(CommitType.TO_STORAGE);
		}
		request.setItems(items);
		try {
			((ThreadedPermissionsManager) PermissionsManager.get())
					.pushSystemUser();
			TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
					request, map, persistTransforms, false, false,
					transformExceptionPolicy);
			return submitAndHandleTransforms(persistenceToken);
		} finally {
			PermissionsManager.get().popUser();
		}
	}

	public List<ServerValidator> validateOnServer(
			List<ServerValidator> validators) throws WebException {
		return ServletLayerLocator.get().commonPersistenceProvider()
				.getCommonPersistence().validate(validators);
	}

	private void logTransformException(DomainTransformResponse response) {
		logger.info(String.format("domain transform problem - clientInstance: %s - rqId: %s - user ",
				response.getRequest().getClientInstance().getId(),response.getRequestId(), PermissionsManager.get().getUserName()));
		List<DomainTransformException> transformExceptions = response
				.getTransformExceptions();
		for (DomainTransformException ex : transformExceptions) {
			logger.info("Per-event error: " + ex.getMessage());
			logger.info("Event: " + ex.getEvent());
		}
	}

	protected void checkAnnotatedPermissions(Object o) {
		AuthenticationRequired ara = o.getClass().getAnnotation(
				AuthenticationRequired.class);
		if (ara != null) {
			if (!PermissionsManager.get().isPermissible(
					new AnnotatedPermissible(ara.permission()))) {
				WrappedRuntimeException e = new WrappedRuntimeException(
						"Permission denied for action " + o,
						SuggestedAction.NOTIFY_WARNING);
				EntityLayerLocator.get().log(
						LogMessageType.TRANSFORM_EXCEPTION,
						"Domain transform permissions exception", e);
				throw e;
			}
		}
	}

	@Override
	protected void doUnexpectedFailure(Throwable e) {
		if (e.getClass().getName().equals(
				"org.apache.catalina.connector.ClientAbortException")) {
			getLogger().debug("Client RPC call aborted by client");
			return;
		}
		super.doUnexpectedFailure(e);
	}

	protected int nextTransformRequestId() {
		return CommonRemoteServiceServletSupport.get().nextTransformRequestId();
	}

	protected abstract void processValidLogin(LoginResponse lrb, String userName)
			throws AuthenticationException;

	protected void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * synchronizing implies serialized transforms per clientInstance
	 */
	protected DomainTransformLayerWrapper transform(
			DomainTransformRequest request, boolean ignoreClientAuthMismatch,
			PersistenceLayerTransformExceptionPolicy transformExceptionPolicy)
			throws DomainTransformRequestException {
		HiliLocatorMap locatorMap = CommonRemoteServiceServletSupport.get()
				.getLocatorMapForClient(request);
		synchronized (locatorMap) {
			TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
					request, locatorMap, true, true, ignoreClientAuthMismatch,
					transformExceptionPolicy);
			return submitAndHandleTransforms(persistenceToken);
		}
	}

	protected DomainTransformLayerWrapper submitAndHandleTransforms(
			TransformPersistenceToken persistenceToken)
			throws DomainTransformRequestException {
		DomainTransformRequestPersistenceSupport persistenceSupport = CommonRemoteServiceServletSupport
				.get().getRequestPersistenceSupport();
		persistenceSupport
				.fireDomainTransformRequestPersistenceEvent(new DomainTransformRequestPersistenceEvent(
						persistenceToken, null));
		DomainTransformLayerWrapper wrapper = ServletLayerLocator.get()
				.transformPersistenceQueue().submit(persistenceToken);
		wrapper.ignored = persistenceToken.ignored;
		persistenceSupport
				.fireDomainTransformRequestPersistenceEvent(new DomainTransformRequestPersistenceEvent(
						persistenceToken, wrapper));
		if (wrapper.response.getResult() == DomainTransformResponseResult.OK) {
			return wrapper;
		} else {
			logTransformException(wrapper.response);
			throw new DomainTransformRequestException(wrapper.response);
		}
	}
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.csobjects.LoginResponseBean;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemResult;
import cc.alcina.framework.common.client.csobjects.ObjectCacheItemSpec;
import cc.alcina.framework.common.client.csobjects.SearchResultsBase;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.common.client.gwittir.validator.ServerValidator;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransform.DataTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DataTransformRequest.DataTransformRequestType;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.AuthenticationRequired;
import cc.alcina.framework.common.client.logic.permissions.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.remote.CommonRemoteService;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.actions.RequiresHttpRequest;
import cc.alcina.framework.entity.datatransform.DataTransformLayerWrapper;
import cc.alcina.framework.entity.datatransform.DataTransformRequestPersistent;
import cc.alcina.framework.entity.datatransform.EntityLayerLocator;
import cc.alcina.framework.entity.datatransform.ThreadlocalTransformManager.HiliLocatorMap;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.servlet.CookieHelper;
import cc.alcina.framework.servlet.ServerLayerLocator;
import cc.alcina.framework.servlet.ServerLayerRegistry;
import cc.alcina.framework.servlet.SessionHelper;
import cc.alcina.framework.servlet.authentication.AuthenticationException;
import cc.alcina.framework.servlet.job.JobRegistry;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;

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
	 * the instance used by the server layer when acting as a client to the ejb
	 * layer
	 */
	public static ClientInstance serverAsClientInstance;

	public static Map<Long, HiliLocatorMap> clientInstanceLocatorMap = new HashMap<Long, HiliLocatorMap>();

	/**
	 * ibid
	 */
	private static int transformRequestCounter = 1;

	protected abstract void processValidLogin(LoginResponseBean lrb,
			String userName) throws AuthenticationException;

	public List<ServerValidator> validateOnServer(
			List<ServerValidator> validators) throws WebException {
		return ServerLayerLocator.get().commonPersistenceProvider()
				.getCommonPersistence().validate(validators);
	}

	public SearchResultsBase search(SearchDefinition def, int pageNumber) {
		return ServerLayerLocator.get().commonPersistenceProvider()
				.getCommonPersistence().search(def, pageNumber);
	}

	public <G extends GwtPersistableObject> Long persist(G gwpo)
			throws WebException {
		try {
			return ServerLayerLocator.get().commonPersistenceProvider()
					.getCommonPersistence().persist(gwpo);
		} catch (Exception e) {
			logger.warn(e);
			throw new WebException(e.getMessage());
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

	private Logger logger;

	protected void setLogger(Logger logger) {
		this.logger = logger;
	}

	public Logger getLogger() {
		return logger;
	}

	public Long logClientError(String exceptionToString) {
		return ServerLayerLocator.get().commonPersistenceProvider()
				.getCommonPersistence().log(exceptionToString,
						LogMessageType.CLIENT_EXCEPTION.toString());
	}

	protected int nextTransformRequestId() {
		return transformRequestCounter++;
	}

	/*
	 * TODO - this should probably be integrated more with {transform} - why is
	 * the server layer so special? just another client
	 */
	public DataTransformResponse transformFromServerLayer(
			boolean persistTransforms) throws DataTransformException {
		DataTransformRequest request = new DataTransformRequest();
		HiliLocatorMap map = new HiliLocatorMap();
		request.setClientInstance(serverAsClientInstance);
		request.setRequestId(nextTransformRequestId());
		ArrayList<DataTransformEvent> items = new ArrayList<DataTransformEvent>(
				TransformManager.get().getTransformsByCommitType(
						CommitType.TO_LOCAL_BEAN));
		if (items.isEmpty()) {
			return null;
		}
		for (DataTransformEvent dte : items) {
			dte.setCommitType(CommitType.TO_REMOTE_STORAGE);
		}
		request.setItems(items);
		try {
			((ThreadedPermissionsManager) PermissionsManager.get())
					.pushSystemUser();
			DataTransformLayerWrapper wrapper = ServerLayerLocator.get()
					.commonPersistenceProvider().getCommonPersistence()
					.transform(request, map, persistTransforms, false);
			return wrapper.response;
		} catch (Exception e) {
			logger.info("data transform problem - user "
					+ PermissionsManager.get().getUserName());
			logger.info(request);
			if (e instanceof DataTransformException) {
				throw ((DataTransformException) e);
			} else {
				throw new WrappedRuntimeException(e);
			}
		} finally {
			PermissionsManager.get().popUser();
		}
	}

	public DataTransformResponse transform(DataTransformRequest request)
			throws DataTransformException {
		Long clientInstanceId = request.getClientInstance().getId();
		synchronized (clientInstanceLocatorMap) {
			if (!clientInstanceLocatorMap.containsKey(clientInstanceId)) {
				clientInstanceLocatorMap.put(clientInstanceId,
						new HiliLocatorMap());
			}
		}
		HiliLocatorMap locatorMap = clientInstanceLocatorMap
				.get(clientInstanceId);
		synchronized (locatorMap) {
			try {
				DataTransformLayerWrapper wrapper = ServerLayerLocator.get()
						.commonPersistenceProvider().getCommonPersistence()
						.transform(request, locatorMap, true, true);
				// not necessary if ejb layer is local
				// clientInstanceLocatorMap.put(clientInstanceId,
				// wrapper.locatorMap);
				return wrapper.response;
			} catch (Exception e) {
				logger.info("data transform problem - user "
						+ PermissionsManager.get().getUserName());
				logger.info(request.toStringForError());
				if (e instanceof DataTransformException) {
					throw ((DataTransformException) e);
				} else {
					throw new WrappedRuntimeException(e);
				}
			}
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
						"Data transform permissions exception", e);
				throw e;
			}
		}
	}

	public List<ActionLogItem> getLogsForAction(RemoteAction action,
			Integer count) {
		checkAnnotatedPermissions(action);
		return ServerLayerLocator.get().commonPersistenceProvider()
				.getCommonPersistence().listLogItemsForClass(
						action.getClass().getName(), count);
	}

	public static final String THRD_LOCAL_RPC_RQ = "THRD_LOCAL_RPC_RQ";

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
					LoginResponseBean lrb = new LoginResponseBean();
					lrb.setOk(true);
					processValidLogin(lrb, userName);
				} catch (AuthenticationException e) {
					// ignore
				}
			}
			rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
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
		}
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
				if (object != null) {
					enc.writeObject(object);
					enc.flush();
					xml = new String(os.toByteArray());
					os.reset();
				}
				msg += CommonUtils.format("\t [%1] - %2\n\t   - %3\n", i++,
						object, xml);
			}
		}
		msg += "Stacktrace:\t " + sw.toString();
		CommonPersistenceLocal cpl = ServerLayerLocator.get()
				.commonPersistenceProvider().getCommonPersistence();
		cpl.log(msg, LogMessageType.RPC_EXCEPTION.toString());
	}

	public <T extends HasIdAndLocalId> T getItemById(String className, Long id)
			throws WebException {
		try {
			Class<T> clazz = (Class<T>) Class.forName(className);
			return ServerLayerLocator.get().commonPersistenceProvider()
					.getCommonPersistence().getItemById(clazz, id, true, false);
		} catch (Exception e) {
			throw new WebException(e.getMessage());
		}
	}

	private int actionCount = 0;

	public Long performAction(final RemoteAction action) {
		return performAction(action, true);
	}

	public Long performAction(final RemoteAction action,
			final boolean persistentLog) {
		checkAnnotatedPermissions(action);
		final RemoteActionPerformer performer = (RemoteActionPerformer) ServerLayerRegistry
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
					ServerLayerLocator.get().commonPersistenceProvider()
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
		RemoteActionPerformer performer = (RemoteActionPerformer) ServerLayerRegistry
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
				ServerLayerLocator.get().commonPersistenceProvider()
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

	public JobInfo pollJobStatus(Long id, boolean cancel) {
		if (cancel) {
			JobRegistry.get().cancel(id);
		}
		return JobRegistry.get().getInfo(id);
	}

	public List<ObjectCacheItemResult> cache(List<ObjectCacheItemSpec> specs)
			throws WebException {
		try {
			return ServerLayerLocator.get().commonPersistenceProvider()
					.getCommonPersistence().cache(specs);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebException(e);
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
		CommonPersistenceLocal cp = ServerLayerLocator.get()
				.commonPersistenceProvider().getCommonPersistence();
		try {
			Class<? extends ClientInstance> clientInstanceClass = cp
					.getImplementation(ClientInstance.class);
			Class<? extends DataTransformRequestPersistent> dtrClass = cp
					.getImplementation(DataTransformRequestPersistent.class);
			long currentClientInstanceId = 0;
			for (DTRSimpleSerialWrapper wr : uncommitted) {
				long clientInstanceId = wr.getClientInstanceId();
				int requestId = (int) wr.getRequestId();
				DataTransformRequest alreadyWritten = cp
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
				DataTransformRequest rq = new DataTransformRequest();
				rq
						.setDataTransformRequestType(DataTransformRequestType.CLIENT_STARTUP_FROM_OFFLINE);
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
				if (logger != null) {
					logger.info(CommonUtils.format(
							"Request [%1/%2] : %3 transforms written",
							requestId, clientInstanceId, rq.getItems().size()));
				}
				transform(rq);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebException(e);
		}
	}

	public List<Long> listRunningJobs() {
		return JobRegistry.get().getRunningJobs();
	}
}

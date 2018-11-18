package cc.alcina.framework.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gwt.event.shared.UmbrellaException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestTagProvider;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.gwt.persistence.client.DTESerializationPolicy;
import cc.alcina.framework.servlet.actionhandlers.DtrSimpleAdminPersistenceHandler;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

public class ServletLayerUtils {
	public static final transient String CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH = ServletLayerUtils.class
			.getName() + ".CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH";

	public static final transient String CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK = ServletLayerUtils.class
			.getName() + ".CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK";

	private static boolean appServletInitialised;

	public static String defaultTag;

	public static boolean checkForBrokenClientPipe(Exception e) {
		return SEUtilities.getFullExceptionMessage(e).contains("Broken pipe");
	}

	public static boolean isAppServletInitialised() {
		return appServletInitialised;
	}

	public static void logRequest(HttpServletRequest req, String remoteAddr) {
		System.out.format(
				"\nRequest: %s\t Querystring: %s\t Referer: %s\t Ip: %s\n",
				req.getRequestURI(), req.getQueryString(),
				req.getHeader("referer"), remoteAddr);
	}

	public static DomainTransformLayerWrapper pushTransforms(String tag,
			boolean asRoot, boolean returnResponse) {
		if (tag == null) {
			tag = DomainTransformRequestTagProvider.get().getTag();
		}
		int pendingTransformCount = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
		if (pendingTransformCount == 0) {
			ThreadlocalTransformManager.cast().resetTltm(null);
			return new DomainTransformLayerWrapper();
		}
		if (AppPersistenceBase.isTest() && !ResourceUtilities
				.is(ServletLayerUtils.class, "testTransformCascade")) {
			return new DomainTransformLayerWrapper();
		}
		int maxTransformChunkSize = ResourceUtilities.getInteger(
				ServletLayerUtils.class, "maxTransformChunkSize", 10000);
		if (pendingTransformCount > maxTransformChunkSize
				&& !LooseContext.is(CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK)) {
			commitLocalTransformsInChunks(maxTransformChunkSize);
			return new DomainTransformLayerWrapper();
		}
		return doPersistTransforms(tag, asRoot);
	}

	public static long pushTransformsAndGetFirstCreationId(boolean asRoot) {
		DomainTransformResponse transformResponse = pushTransforms(null, asRoot,
				true).response;
		DomainTransformEvent first = CommonUtils
				.first(transformResponse.getEventsToUseForClientUpdate());
		return first == null ? 0 : first.getGeneratedServerId();
	}

	public static long pushTransformsAndReturnId(boolean asRoot,
			HasIdAndLocalId returnIdFor) {
		DomainTransformResponse transformResponse = pushTransforms(null, asRoot,
				true).response;
		for (DomainTransformEvent dte : transformResponse
				.getEventsToUseForClientUpdate()) {
			if (dte.getObjectLocalId() == returnIdFor.getLocalId()
					&& dte.getObjectClass() == returnIdFor.getClass()
					&& dte.getTransformType() == TransformType.CREATE_OBJECT) {
				return dte.getGeneratedServerId();
			}
		}
		throw new RuntimeException(
				"Generated object not found - " + returnIdFor);
	}

	public static int pushTransformsAsCurrentUser() {
		return pushTransforms(false);
	}

	public static int pushTransformsAsRoot() {
		return pushTransforms(true);
	}

	public static String robustGetRemoteAddr(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		return CommonUtils.isNotNullOrEmpty(forwarded) ? forwarded
				: request.getRemoteAddr();
	}

	public static void setAppServletInitialised(boolean appServletInitialised) {
		ServletLayerUtils.appServletInitialised = appServletInitialised;
	}

	public static void setLoggerLevels() {
		Logger.getLogger("org.apache.kafka").setLevel(Level.WARN);
		Logger.getLogger("org.apache.http").setLevel(Level.WARN);
		Logger.getLogger("org.apache.http.wire").setLevel(Level.WARN);
		Logger.getLogger("httpclient.wire.header").setLevel(Level.WARN);
		Logger.getLogger("httpclient.wire.content").setLevel(Level.WARN);
		Logger.getRootLogger().setLevel(Level.WARN);
	}

	private static void
			commitLocalTransformsInChunks(final int maxTransformChunkSize) {
		try {
			Callable looper = new Callable() {
				@Override
				public Object call() throws Exception {
					String ipAddress = null;
					HttpServletRequest contextThreadLocalRequest = CommonRemoteServiceServlet
							.getContextThreadLocalRequest();
					long extClientInstanceId = 0;
					if (contextThreadLocalRequest != null) {
						ipAddress = ServletLayerUtils
								.robustGetRemoteAddr(contextThreadLocalRequest);
						extClientInstanceId = PermissionsManager.get()
								.getClientInstance().getId();
					}
					final ClientInstance commitInstance = Registry
							.impl(CommonPersistenceProvider.class)
							.getCommonPersistence()
							.createClientInstance(Ax.format(
									"servlet-bulk: %s - derived from client instance : %s",
									EntityLayerUtils.getLocalHostName(),
									extClientInstanceId), null, ipAddress);
					List<DomainTransformEvent> transforms = new ArrayList<DomainTransformEvent>(
							TransformManager.get().getTransformsByCommitType(
									CommitType.TO_LOCAL_BEAN));
					TransformManager.get()
							.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN)
							.clear();
					ThreadlocalTransformManager.cast().resetTltm(null);
					DomainTransformRequest rq = new DomainTransformRequest();
					rq.setProtocolVersion(new DTESerializationPolicy()
							.getTransformPersistenceProtocol());
					rq.setRequestId(1);
					rq.setClientInstance(commitInstance);
					rq.setEvents(transforms);
					DeltaApplicationRecord dar = new DeltaApplicationRecord(rq,
							DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED,
							false);
					DtrSimpleAdminPersistenceHandler persistenceHandler = new DtrSimpleAdminPersistenceHandler();
					persistenceHandler.commit(dar, maxTransformChunkSize);
					Exception ex = persistenceHandler.getJobTracker()
							.getJobException();
					if (ex != null) {
						throw ex;
					}
					return null;
				}
			};
			ThreadedPermissionsManager.cast()
					.callWithPushedSystemUserIfNeeded(looper);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			ThreadlocalTransformManager.cast().resetTltm(null);
		}
	}

	private static int pushTransforms(boolean asRoot) {
		int pendingTransformCount = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
		if (AppPersistenceBase.isTest() && !ResourceUtilities
				.is(ServletLayerUtils.class, "testTransformCascade")) {
			if (!LooseContext.is(CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH)) {
				TransformManager.get().clearTransforms();
			}
			return pendingTransformCount;
		}
		pushTransforms(null, asRoot, true);
		return pendingTransformCount;
	}

	protected static DomainTransformLayerWrapper doPersistTransforms(String tag,
			boolean asRoot) {
		// for debugging
		Set<DomainTransformEvent> transforms = TransformManager.get()
				.getTransforms();
		ThreadedPermissionsManager tpm = ThreadedPermissionsManager.cast();
		boolean muted = MetricLogging.get().isMuted();
		try {
			MetricLogging.get().setMuted(true);
			if (asRoot) {
				tpm.pushSystemUser();
			} else {
				tpm.pushCurrentUser();
			}
			CascadingTransformSupport cascadingTransformSupport = CascadingTransformSupport
					.get();
			try {
				cascadingTransformSupport.beforeTransform();
				DomainTransformLayerWrapper wrapper = Registry
						.impl(CommonRemoteServletProvider.class)
						.getCommonRemoteServiceServlet()
						.transformFromServletLayer(tag);
				// see preamble to cascading transform support
				while (cascadingTransformSupport.hasChildren()) {
					synchronized (cascadingTransformSupport) {
						if (cascadingTransformSupport.hasChildren()) {
							cascadingTransformSupport.wait();
						}
					}
				}
				UmbrellaException childException = cascadingTransformSupport
						.getException();
				if (childException != null) {
					throw childException;
				}
				return wrapper;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			} finally {
				cascadingTransformSupport.afterTransform();
			}
		} finally {
			tpm.popUser();
			ThreadlocalTransformManager.cast().resetTltm(null);
			MetricLogging.get().setMuted(muted);
		}
	}
}

package cc.alcina.framework.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.gwt.persistence.client.DTESerializationPolicy;
import cc.alcina.framework.servlet.actionhandlers.DtrSimpleAdminPersistenceHandler;

import com.google.gwt.event.shared.UmbrellaException;

public class ServletLayerUtils {
	public static final transient String CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH = ServletLayerUtils.class
			.getName() + ".CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH";

	public static final transient String CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK = ServletLayerUtils.class
			.getName() + ".CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK";

	public static int pushTransformsAsRoot() {
		return pushTransforms(true);
	}

	public static int pushTransformsAsCurrentUser() {
		return pushTransforms(false);
	}

	private static int pushTransforms(boolean asRoot) {
		int pendingTransformCount = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
		if (AppPersistenceBase.isTest()) {
			if (!LooseContext.is(CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH)) {
				TransformManager.get().clearTransforms();
			}
			return pendingTransformCount;
		}
		pushTransforms(null, asRoot, true);
		return pendingTransformCount;
	}

	public static DomainTransformLayerWrapper pushTransforms(String tag,
			boolean asRoot, boolean returnResponse) {
		int pendingTransformCount = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
		if (pendingTransformCount == 0) {
			ThreadlocalTransformManager.cast().resetTltm(null);
			return new DomainTransformLayerWrapper();
		}
		if (AppPersistenceBase.isTest()) {
			return new DomainTransformLayerWrapper();
		}
		int maxTransformChunkSize = ResourceUtilities.getInteger(
				ServletLayerUtils.class, "maxTransformChunkSize", 6000);
		if (pendingTransformCount > maxTransformChunkSize
				&& !LooseContext.is(CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK)) {
			commitLocalTransformsInChunks(maxTransformChunkSize);
			return new DomainTransformLayerWrapper();
		}
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

	private static void commitLocalTransformsInChunks(
			final int maxTransformChunkSize) {
		try {
			Callable looper = new Callable() {
				@Override
				public Object call() throws Exception {
					final ClientInstance commitInstance = Registry
							.impl(CommonPersistenceProvider.class)
							.getCommonPersistence()
							.createClientInstance(
									"servlet-bulk: "
											+ ServletLayerUtils
													.getLocalHostName());
					List<DomainTransformEvent> transforms = new ArrayList<DomainTransformEvent>(
							TransformManager.get().getTransformsByCommitType(
									CommitType.TO_LOCAL_BEAN));
					TransformManager
							.get()
							.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN)
							.clear();
					ThreadlocalTransformManager.cast().resetTltm(null);
					DomainTransformRequest rq = new DomainTransformRequest();
					rq.setProtocolVersion(new DTESerializationPolicy()
							.getTransformPersistenceProtocol());
					rq.setRequestId(1);
					rq.setClientInstance(commitInstance);
					rq.setEvents(transforms);
					DeltaApplicationRecord dar = new DeltaApplicationRecord(
							rq,
							DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED,
							false);
					new DtrSimpleAdminPersistenceHandler().commit(dar,
							maxTransformChunkSize);
					return null;
				}
			};
			ThreadedPermissionsManager.cast().runWithPushedSystemUserIfNeeded(
					looper);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			ThreadlocalTransformManager.cast().resetTltm(null);
		}
	}

	public static long pushTransformsAndGetFirstCreationId(boolean asRoot) {
		DomainTransformResponse transformResponse = pushTransforms(null,
				asRoot, true).response;
		DomainTransformEvent first = CommonUtils.first(transformResponse
				.getEventsToUseForClientUpdate());
		return first == null ? 0 : first.getGeneratedServerId();
	}

	public static long pushTransformsAndReturnId(boolean asRoot,
			HasIdAndLocalId returnIdFor) {
		DomainTransformResponse transformResponse = pushTransforms(null,
				asRoot, true).response;
		for (DomainTransformEvent dte : transformResponse
				.getEventsToUseForClientUpdate()) {
			if (dte.getObjectLocalId() == returnIdFor.getLocalId()
					&& dte.getObjectClass() == returnIdFor.getClass()
					&& dte.getTransformType() == TransformType.CREATE_OBJECT) {
				return dte.getGeneratedServerId();
			}
		}
		throw new RuntimeException("Generated object not found - "
				+ returnIdFor);
	}

	public static void logRequest(HttpServletRequest req, String remoteAddr) {
		System.out.format(
				"\nRequest: %s\t Querystring: %s\t Referer: %s\t Ip: %s\n",
				req.getRequestURI(), req.getQueryString(),
				req.getHeader("referer"), remoteAddr);
	}

	public static String getLocalHostName() {
		try {
			return java.net.InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}

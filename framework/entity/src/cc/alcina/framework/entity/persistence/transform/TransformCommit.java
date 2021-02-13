package cc.alcina.framework.entity.persistence.transform;

import java.io.File;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.event.shared.UmbrellaException;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequestTagProvider;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse.DomainTransformResponseResult;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DeltaApplicationRecordSerializerImpl;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerTransformPropagation;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.WrappedObject;
import cc.alcina.framework.entity.persistence.cache.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.RemoteTransformPersister;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.TransformConflicts;
import cc.alcina.framework.entity.transform.TransformConflicts.TransformConflictsFromOfflineSupport;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.entity.transform.policy.TransformPropagationPolicy;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.gwt.persistence.client.DTESerializationPolicy;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = TransformCommit.class, implementationType = ImplementationType.SINGLETON)
public class TransformCommit {
	private static final String TOPIC_UNEXPECTED_TRANSFORM_PERSISTENCE_EXCEPTION = TransformCommit.class
			.getName() + ".TOPIC_UNEXPECTED_TRANSFORM_PERSISTENCE_EXCEPTION";

	public static final transient String CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH = TransformCommit.class
			.getName() + ".CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH";

	public static final transient String CONTEXT_TRANSFORM_PRIORITY = TransformCommit.class
			.getName() + ".CONTEXT_TRANSFORM_PRIORITY";

	public static final transient String CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK = TransformCommit.class
			.getName() + ".CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK";

	private static final transient String CONTEXT_COMMIT_CLIENT_INSTANCE_CONTEXT = TransformCommit.class
			.getName() + ".CONTEXT_COMMIT_CLIENT_INSTANCE_CONTEXT";

	public static final String CONTEXT_USE_WRAPPER_USER_WHEN_PERSISTING_OFFLINE_TRANSFORMS = TransformCommit.class
			.getName() + "."
			+ "CONTEXT_USE_WRAPPER_USER_WHEN_PERSISTING_OFFLINE_TRANSFORMS";

	public static final String CONTEXT_REUSE_IUSER_HOLDER = TransformCommit.class
			.getName() + ".CONTEXT_REUSE_IUSER_HOLDER";

	public static final transient String CONTEXT_DISABLED = TransformCommit.class
			.getName() + ".CONTEXT_DISABLED";

	static Logger logger = LoggerFactory.getLogger(TransformCommit.class);

	public static int commitBulkTransforms(List<DeltaApplicationRecord> records,
			Boolean useWrapperUser, boolean throwPersistenceExceptions)
			throws WebException {
		CommonPersistenceLocal cp = Registry
				.impl(CommonPersistenceProvider.class).getCommonPersistence();
		boolean persistAsOneTransaction = ResourceUtilities.is(
				TransformCommit.class,
				"persistOfflineTransformsAsOneTransaction");
		try {
			// save a copy of the records
			{
				String folderName = Ax.format("cli_%s_time_%s",
						records.get(0).getClientInstanceId(), CommonUtils
								.formatDate(new Date(), DateStyle.TIMESTAMP));
				File offlineDir = DataFolderProvider.get()
						.getChildFile("offlineTransforms-partial");
				File saveDir = SEUtilities.getChildFile(offlineDir, folderName);
				saveDir.mkdirs();
				DeltaApplicationRecordSerializerImpl recordSerializer = new DeltaApplicationRecordSerializerImpl();
				for (DeltaApplicationRecord record : records) {
					int id = record.getRequestId();
					long clientInstanceId = record.getClientInstanceId();
					String fileName = String.format("%s_%s_ser.txt",
							clientInstanceId, id);
					File out = SEUtilities.getChildFile(saveDir, fileName);
					ResourceUtilities.write(recordSerializer.write(record),
							out);
				}
				logger.info("Wrote {} offline/bulk records to {}",
						records.size(), saveDir);
			}
			Class<? extends ClientInstance> clientInstanceClass = PersistentImpl
					.getImplementation(ClientInstance.class);
			Class<? extends DomainTransformRequestPersistent> dtrClass = PersistentImpl
					.getImplementation(DomainTransformRequestPersistent.class);
			long currentClientInstanceId = 0;
			int committed = 0;
			LooseContext.getContext().pushWithKey(
					TransformConflicts.CONTEXT_OFFLINE_SUPPORT,
					new TransformConflictsFromOfflineSupport());
			ReuseIUserHolder reuseIUserHolder = LooseContext
					.get(TransformCommit.CONTEXT_REUSE_IUSER_HOLDER);
			IUser wrapperUser = reuseIUserHolder == null ? null
					: reuseIUserHolder.iUser;
			long idCounter = 1;
			List<DomainTransformRequest> toCommit = new ArrayList<>();
			for (int idx = 0; idx < records.size(); idx++) {
				DeltaApplicationRecord deltaRecord = records.get(idx);
				long clientInstanceId = deltaRecord.getClientInstanceId();
				int requestId = deltaRecord.getRequestId();
				DomainTransformRequest alreadyWritten = cp
						.getItemByKeyValueKeyValue(dtrClass,
								"clientInstance.id", clientInstanceId,
								"requestId", requestId);
				if (alreadyWritten != null) {
					logger.warn(Ax.format("Request [{}/{}] already written",
							requestId, clientInstanceId));
					continue;
				}
				DomainTransformRequest request = DomainTransformRequest
						.fromString(deltaRecord.getText(),
								deltaRecord.getChunkUuidString());
				ClientInstance clientInstance = null;
				if (deltaRecord.getClientInstanceId() == ClientInstance.self()
						.getId()) {
					clientInstance = ClientInstance.self();
				} else {
					clientInstance = clientInstanceClass.newInstance();
					clientInstance.setAuth(deltaRecord.getClientInstanceAuth());
					clientInstance.setId(deltaRecord.getClientInstanceId());
				}
				request.setClientInstance(clientInstance);
				boolean committingUserIsAdministrator = UserlandProvider.get()
						.getGroupByName(
								PermissionsManager.ADMINISTRATORS_GROUP_NAME)
						.containsUserOrMemberGroupContainsUser(UserlandProvider
								.get().getUserById(deltaRecord.getUserId()));
				if (useWrapperUser == null) {
					useWrapperUser = committingUserIsAdministrator
							&& LooseContext.getContext().getBoolean(
									TransformCommit.CONTEXT_USE_WRAPPER_USER_WHEN_PERSISTING_OFFLINE_TRANSFORMS)
							&& deltaRecord.getUserId() != PermissionsManager
									.get().getUserId();
				}
				DomainTransformLayerWrapper transformLayerWrapper;
				// NOTE - at the moment, if all records are pushed in one
				// transaction, just the last clientInstance is used
				request.setRequestId(deltaRecord.getRequestId());
				request.setTag(deltaRecord.getTag());
				// necessary because event id is used by transformpersister
				// for
				// pass control etc
				for (DomainTransformEvent event : request.getEvents()) {
					event.setEventId(idCounter++);
					event.setCommitType(CommitType.TO_STORAGE);
				}
				try {
					if (useWrapperUser) {
						if (!PermissionsManager.get().isAdmin()) {
							try {
								LooseContext.pushWithTrue(
										AuthenticationPersistence.CONTEXT_IDLE_TIMEOUT_DISABLED);
								if (!AuthenticationPersistence.get()
										.validateClientInstance(
												deltaRecord
														.getClientInstanceId(),
												deltaRecord
														.getClientInstanceAuth())) {
									throw new RuntimeException(
											"invalid wrapper authentication");
								}
							} finally {
								LooseContext.pop();
							}
						}
						if (wrapperUser != null && wrapperUser
								.getId() == deltaRecord.getUserId()) {
						} else {
							wrapperUser = Domain.find(
									PersistentImpl
											.getImplementation(IUser.class),
									deltaRecord.getUserId());
							if (reuseIUserHolder != null) {
								reuseIUserHolder.iUser = wrapperUser;
							}
						}
						if (wrapperUser == null) {
							// admin persistence
							wrapperUser = PermissionsManager.get().getUser();
						}
						boolean asRoot = wrapperUser == UserlandProvider.get()
								.getSystemUser();
						PermissionsManager.get().pushUser(wrapperUser,
								LoginState.LOGGED_IN, asRoot);
					} else {
						if (!Objects.equals(
								Domain.find(request.getClientInstance())
										.provideUser(),
								PermissionsManager.get().getUser())) {
							throw new UnsupportedOperationException(
									"May need to create an additional authenticationSession");
							// request.getClientInstance()
							// .setUser(PermissionsManager.get().getUser());
						}
					}
					boolean last = idx == records.size() - 1;
					if (!persistAsOneTransaction || last) {
						if (last) {
							request.getPriorRequestsWithoutResponse()
									.addAll(toCommit);
						}
						transformLayerWrapper = MethodContext.instance()
								.withContextTrue(
										AdjunctTransformCollation.CONTEXT_TM_TRANSFORMS_ARE_EX_THREAD)
								.call(() -> get().transform(request, true, true,
										true));
						ThreadlocalTransformManager.cast().resetTltm(null);
						logger.info(
								"Request [{}::{}] : {} transforms written, {} ignored",
								requestId, clientInstanceId,
								transformLayerWrapper.response
										.getTransformsProcessed(),
								transformLayerWrapper.ignored);
						if (throwPersistenceExceptions
								&& !transformLayerWrapper.response
										.getTransformExceptions().isEmpty()) {
							throw (transformLayerWrapper.response
									.getTransformExceptions().get(0));
						}
					} else {
						toCommit.add(request);
					}
				} finally {
					if (useWrapperUser) {
						PermissionsManager.get().popUser();
					}
				}
				committed++;
			}
			return committed;
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebException(e);
		} finally {
			LooseContext.getContext().pop();
		}
	}

	// Note that this must be called with a new client instance - since we're
	// incrementing the request id counter
	public static void commitDeltaApplicationRecord(DeltaApplicationRecord dar,
			int chunkSize) throws Exception {
		DomainTransformRequest fullRequest = DomainTransformRequest
				.fromString(dar.getText(), dar.getChunkUuidString());
		int size = fullRequest.getEvents().size();
		boolean commitAsWrapperUser = ResourceUtilities
				.is("commitAsWrapperUser");
		boolean committingVmLocalRecord = dar
				.getClientInstanceId() == ClientInstance.self().getId();
		if (size > chunkSize && dar.getChunkUuidString() != null) {
			int rqIdCounter = dar.getRequestId();
			for (int idx = 0; idx < size;) {
				IntPair range = null;
				if (idx + chunkSize > size) {
					range = new IntPair(idx, size);
				} else {
					int createSearchIdx = idx + chunkSize;
					int maxCreateIdxDelta = size / 2;
					for (; createSearchIdx < size && maxCreateIdxDelta > 0;) {
						DomainTransformEvent evt = fullRequest.getEvents()
								.get(createSearchIdx);
						if (evt.getTransformType() == TransformType.CREATE_OBJECT) {
							// i.e. trim the range to just before this
							// create event
							range = new IntPair(idx, createSearchIdx);
							break;
						}
						if (evt.getTransformType() == TransformType.DELETE_OBJECT) {
							// i.e. trim the range to just after this create
							// event
							range = new IntPair(idx, createSearchIdx + 1);
							break;
						}
						createSearchIdx++;
						maxCreateIdxDelta--;
					}
					if (range == null) {
						range = new IntPair(idx, idx + chunkSize);
					}
				}
				DomainTransformRequest chunkRequest = DomainTransformRequest
						.createSubRequest(fullRequest, range);
				// null UUID, we'll set it in a bit
				int nextRequestId = committingVmLocalRecord
						? get().nextTransformRequestId()
						: rqIdCounter++;
				DeltaApplicationRecord chunk = new DeltaApplicationRecord(0, "",
						dar.getTimestamp(), dar.getUserId(),
						dar.getClientInstanceId(), nextRequestId,
						dar.getClientInstanceAuth(),
						DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED,
						dar.getProtocolVersion(), dar.getTag(),
						chunkRequest.getChunkUuidString());
				List<DomainTransformEvent> subList = fullRequest.getEvents()
						.subList(range.i1, range.i2);
				chunkRequest.setRequestId(chunk.getRequestId());
				chunkRequest.setEvents(
						new ArrayList<DomainTransformEvent>(subList));
				chunk.setText(chunkRequest.toString());
				commitBulkTransforms(
						Arrays.asList(new DeltaApplicationRecord[] { chunk }),
						commitAsWrapperUser, true);
				String message = String.format(
						"written chunk - writing chunk %s of %s", range, size);
				System.out.println(message);
				idx = range.i2;
			}
		} else {
			dar.setType(DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED);
			commitBulkTransforms(
					Arrays.asList(new DeltaApplicationRecord[] { dar }),
					commitAsWrapperUser, true);
		}
	}

	public static void
			commitLocalTransformsInChunks(int maxTransformChunkSize) {
		try {
			ThreadedPermissionsManager.cast()
					.runThrowingWithPushedSystemUserIfNeeded(
							() -> get().commitLocalTranformInChunks0(
									maxTransformChunkSize));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			ThreadlocalTransformManager.cast().resetTltm(null);
		}
	}

	public static DomainTransformLayerWrapper commitTransforms(String tag,
			boolean asRoot, boolean returnResponse) {
		if (tag == null) {
			tag = DomainTransformRequestTagProvider.get().getTag();
		}
		int cleared = TransformManager.get().removeCreateDeleteTransforms();
		if (cleared != 0) {
			logger.trace("Cleared {} created/deleted transforms", cleared);
		}
		int pendingTransformCount = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
		if (pendingTransformCount == 0) {
			ThreadlocalTransformManager.cast().resetTltm(null);
			Transaction.endAndBeginNew();
			return new DomainTransformLayerWrapper(null);
		}
		if (Ax.isTest() && !isTestTransformCascade()) {
			return new DomainTransformLayerWrapper(null);
		}
		if (Ax.isTest() && LooseContext.is(CONTEXT_DISABLED)) {
			return new DomainTransformLayerWrapper(null);
		}
		if (Ax.isTest() && EntityLayerObjects.get()
				.getServerAsClientInstance() == null) {
			// pre-login test tx (say fixing up credentials) - create dummy
			ThreadlocalTransformManager.cast().resetTltm(null);
			return new DomainTransformLayerWrapper(null);
		}
		int maxTransformChunkSize = ResourceUtilities
				.getInteger(TransformCommit.class, "maxTransformChunkSize");
		/*
		 * If context not set (by http request), it's from the server
		 */
		LooseContext.ensure(CONTEXT_COMMIT_CLIENT_INSTANCE_CONTEXT,
				() -> new CommitClientInstanceContext(
						EntityLayerObjects.get().getServerAsClientInstance()
								.getId(),
						PermissionsManager.get().getUserId(), "0.0.0.0"));
		long persistentTransformRecordCount = Registry
				.impl(TransformPropagationPolicy.class)
				.getProjectedPersistentCount(TransformManager.get()
						.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN)
						.stream());
		if (persistentTransformRecordCount > maxTransformChunkSize
				&& !LooseContext.is(CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK)) {
			commitLocalTransformsInChunks(maxTransformChunkSize);
			return new DomainTransformLayerWrapper(null);
		}
		return get().doPersistTransforms(tag, asRoot);
	}

	public static long commitTransformsAndGetFirstCreationId(boolean asRoot) {
		DomainTransformResponse transformResponse = commitTransforms(null,
				asRoot, true).response;
		DomainTransformEvent first = CommonUtils
				.first(transformResponse.getEventsToUseForClientUpdate());
		return first == null ? 0 : first.getGeneratedServerId();
	}

	public static long commitTransformsAndReturnId(boolean asRoot,
			Entity returnIdFor) {
		DomainTransformResponse transformResponse = commitTransforms(null,
				asRoot, true).response;
		for (DomainTransformEvent dte : transformResponse
				.getEventsToUseForClientUpdate()) {
			if (dte.getObjectLocalId() == returnIdFor.getLocalId()
					&& dte.getObjectClass() == returnIdFor.entityClass()
					&& dte.getTransformType() == TransformType.CREATE_OBJECT) {
				return dte.getGeneratedServerId();
			}
		}
		throw new RuntimeException(
				"Generated object not found - " + returnIdFor);
	}

	public static int commitTransformsAsCurrentUser() {
		return commitTransforms(false);
	}

	public static int commitTransformsAsRoot() {
		return commitTransforms(true);
	}

	public static void commitWithBackoff() {
		commitWithBackoff(0, 5, 40, 2.0);
	}

	public static void commitWithBackoff(int initialDelayMs, int retries,
			double delayMs, double retryMultiplier) {
		try {
			Thread.sleep(initialDelayMs);
			while (retries-- > 0) {
				try {
					commitTransforms(true);
					break;
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
					logger.warn("Exception in commitWithBackoff, retrying");
					Thread.sleep((long) delayMs);
					delayMs *= (0.5 + Math.random()) * retryMultiplier;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void enqueueTransforms(String transformQueueName,
			Class<? extends Entity>... entityClassNames) {
		List<DomainTransformEvent> transforms = removeTransforms(
				entityClassNames);
		BackendTransformQueue.get().enqueue(transforms, transformQueueName);
	}

	public static TransformCommit get() {
		return Registry.impl(TransformCommit.class);
	}

	public static boolean isCommitTestTransforms() {
		return ResourceUtilities.is("commitTestTransforms");
	}

	public static boolean isTestTransformCascade() {
		return ResourceUtilities.is("testTransformCascade");
	}

	public static void prepareHttpRequestCommitContext(long clientInstanceId,
			long userId, String committerIpAddress) {
		LooseContext.set(CONTEXT_COMMIT_CLIENT_INSTANCE_CONTEXT,
				new CommitClientInstanceContext(clientInstanceId, userId,
						committerIpAddress));
	}

	public static List<DomainTransformEvent>
			removeTransforms(Class<? extends Entity>... entityClassNames) {
		Set<Class> toRemove = Arrays.stream(entityClassNames).map(clazz -> {
			if (Modifier.isAbstract(clazz.getModifiers())) {
				return PersistentImpl.getImplementation(clazz);
			}
			return clazz;
		}).collect(Collectors.toSet());
		return TransformManager.get().getTransforms().stream()
				.collect(Collectors.toList()).stream()
				.filter(transform -> toRemove.isEmpty()
						|| toRemove.contains(transform.getObjectClass()))
				.map(TransformManager.get()::removeTransform)
				.collect(Collectors.toList());
	}

	public static Topic<TransformPersistenceToken>
			topicUnexpectedExceptionBeforePostTransform() {
		return Topic.global(TOPIC_UNEXPECTED_TRANSFORM_PERSISTENCE_EXCEPTION);
	}

	private static int commitTransforms(boolean asRoot) {
		int pendingTransformCount = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
		if (pendingTransformCount == 0) {
			return 0;
		}
		if (AppPersistenceBase.isTest()
				&& !TransformCommit.isTestTransformCascade()) {
			if (!LooseContext
					.is(TransformCommit.CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH)) {
				TransformManager.get().clearTransforms();
			}
			return pendingTransformCount;
		}
		DomainTransformLayerWrapper layerWrapper = commitTransforms(null,
				asRoot, true);
		if (layerWrapper.response != null
				&& layerWrapper.response.getTransformExceptions().size() > 0) {
			throw WrappedRuntimeException.wrapIfNotRuntime(
					layerWrapper.response.getTransformExceptions().get(0));
		}
		return pendingTransformCount;
	}

	private Map<Long, EntityLocatorMap> clientInstanceLocatorMap = new ConcurrentHashMap<>();

	private AtomicInteger transformRequestCounter = new AtomicInteger(0);

	public void enqueueBackendTransform(Runnable runnable) {
		enqueueBackendTransform(runnable, null);
	}

	public void enqueueBackendTransform(Runnable runnable, String queueName) {
		BackendTransformQueue.get().enqueue(runnable, queueName);
	}

	public EntityLocatorMap getLocatorMapForClient(
			ClientInstance clientInstance, boolean forceRefresh) {
		Long clientInstanceId = clientInstance.getId();
		if (!clientInstanceLocatorMap.containsKey(clientInstanceId)
				|| forceRefresh) {
			synchronized (clientInstance) {
				EntityLocatorMap locatorMap = CommonPersistenceProvider.get()
						.getCommonPersistence().getLocatorMap(clientInstanceId);
				clientInstanceLocatorMap.put(clientInstanceId, locatorMap);
			}
		}
		EntityLocatorMap locatorMap = clientInstanceLocatorMap
				.get(clientInstanceId);
		return locatorMap;
	}

	public EntityLocatorMap
			getLocatorMapForClient(DomainTransformRequest request) {
		return getLocatorMapForClient(Domain.find(request.getClientInstance()),
				false);
	}

	public void handleWrapperTransforms() {
		EntityLayerTransformPropagation transformPropagation = Registry
				.impl(EntityLayerTransformPropagation.class, void.class, true);
		if (transformPropagation == null) {
			return;
		}
		ThreadlocalTransformManager.cast().getTransforms();
		Set<DomainTransformEvent> pendingTransforms = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
		if (pendingTransforms.isEmpty()) {
			return;
		}
		final List<DomainTransformEvent> items = CollectionFilters
				.filter(pendingTransforms, new IsWrappedObjectDteFilter());
		pendingTransforms.removeAll(items);
		if (!items.isEmpty() && !pendingTransforms.isEmpty()) {
			throw new RuntimeException("Non-wrapped and wrapped object"
					+ " transforms registered after transformPerist()");
		}
		if (items.isEmpty()) {
			return;
		}
		new Thread() {
			@Override
			public void run() {
				try {
					int depth = LooseContext.depth();
					transformFromServletLayer(items, null);
					LooseContext.confirmDepth(depth);
					ThreadlocalTransformManager.cast().resetTltm(null);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			};
		}.start();
	}

	public int nextTransformRequestId() {
		return transformRequestCounter.incrementAndGet();
	}

	public void setBackendTransformQueueMaxDelay(String queueName,
			long delayMs) {
		BackendTransformQueue.get().setBackendTransformQueueMaxDelay(queueName,
				delayMs);
	}

	/**
	 * synchronizing implies serialized transforms per clientInstance
	 */
	public DomainTransformLayerWrapper transform(DomainTransformRequest request,
			boolean ignoreClientAuthMismatch, boolean forOfflineTransforms,
			boolean blockUntilAllListenersNotified)
			throws DomainTransformRequestException {
		EntityLocatorMap locatorMap = getLocatorMapForClient(request);
		synchronized (locatorMap) {
			TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
					request, locatorMap,
					request.getClientInstance() != ClientInstance.self(),
					ignoreClientAuthMismatch, forOfflineTransforms, logger,
					blockUntilAllListenersNotified);
			return submitAndHandleTransforms(persistenceToken);
		}
	}

	public DomainTransformLayerWrapper transformFromServletLayer(
			Collection<DomainTransformEvent> transforms, String tag)
			throws DomainTransformRequestException {
		int requestId = nextTransformRequestId();
		EntityLocatorMap map = new EntityLocatorMap();
		ClientInstance clientInstance = EntityLayerObjects.get()
				.getServerAsClientInstance();
		DomainTransformRequest request = DomainTransformRequest
				.createPersistableRequest(requestId, clientInstance.getId());
		request.setClientInstance(clientInstance);
		request.setTag(tag);
		request.setRequestId(requestId);
		for (DomainTransformEvent dte : transforms) {
			dte.setCommitType(CommitType.TO_STORAGE);
		}
		request.getEvents().addAll(transforms);
		try {
			ThreadedPermissionsManager.cast().pushSystemUser();
			TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
					request, map, false, false, false, logger, true);
			CommitClientInstanceContext clientInstanceContext = LooseContext
					.get(CONTEXT_COMMIT_CLIENT_INSTANCE_CONTEXT);
			persistenceToken.setOriginatingUserId(clientInstanceContext.userId);
			return submitAndHandleTransforms(persistenceToken);
		} finally {
			ThreadedPermissionsManager.cast().popSystemUser();
		}
	}

	public DomainTransformLayerWrapper transformFromServletLayer(String tag)
			throws DomainTransformRequestException {
		Set<DomainTransformEvent> pendingTransforms = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN);
		if (pendingTransforms.isEmpty()) {
			return null;
		}
		ArrayList<DomainTransformEvent> items = new ArrayList<DomainTransformEvent>(
				pendingTransforms);
		pendingTransforms.clear();
		return transformFromServletLayer(items, tag);
	}

	private void commitLocalTranformInChunks0(int maxTransformChunkSize)
			throws Exception {
		CommitClientInstanceContext commitClientInstanceContext = LooseContext
				.get(CONTEXT_COMMIT_CLIENT_INSTANCE_CONTEXT);
		ClientInstance fromInstance = AuthenticationPersistence.get()
				.getClientInstance(
						commitClientInstanceContext.clientInstanceId);
		String uaString = Ax.format(
				"servlet-bulk: %s - derived from client instance : %s",
				EntityLayerUtils.getLocalHostName(),
				commitClientInstanceContext.clientInstanceId);
		List<DomainTransformEvent> transforms = new ArrayList<DomainTransformEvent>(
				TransformManager.get()
						.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN));
		TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).clear();
		ThreadlocalTransformManager.cast().resetTltm(null);
		Transaction.endAndBeginNew();
		/*
		 * There's a method in this apparent madness. The big request for this
		 * (new) client instance, rq id #1, will be committed as several chunks
		 * (#2, #3 etc) - and we need to keep the localId->(persisted)id
		 * correspondence constant for all those chunks, hence the disposable
		 * clientinstance (if the committing user is not the app's
		 * ClientInstance.self())
		 * 
		 * And the reason we need to commit as a new instance (if not self) is
		 * because DomainTransformRequest.requestId must be unique
		 */
		ClientInstance commitInstance = null;
		int requestId = -1;
		if (fromInstance == ClientInstance.self()) {
			// this jvm controls the requestId counter, so just pass the next
			// local request id and the local client instance
			commitInstance = ClientInstance.self();
			requestId = nextTransformRequestId();
		} else {
			// create our temporary instance and requestId series
			commitInstance = AuthenticationPersistence.get()
					.createClientInstance(
							fromInstance.getAuthenticationSession(), uaString,
							commitClientInstanceContext.committerIpAddress,
							null, null);
			MethodContext.instance()
					.withContextTrue(CONTEXT_FORCE_COMMIT_AS_ONE_CHUNK)
					.run(() -> Transaction.commit());
			requestId = 1;
		}
		DomainTransformRequest request = DomainTransformRequest
				.createPersistableRequest(requestId, commitInstance.getId());
		request.setProtocolVersion(
				new DTESerializationPolicy().getTransformPersistenceProtocol());
		request.setRequestId(requestId);
		request.setClientInstance(commitInstance);
		request.setEvents(transforms);
		DeltaApplicationRecord dar = new DeltaApplicationRecord(request,
				DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED, false);
		commitDeltaApplicationRecord(dar, maxTransformChunkSize);
	}

	private void logTransformException(DomainTransformResponse response) {
		logger.warn(String.format(
				"domain transform problem - clientInstance: %s - rqId: %s - user ",
				response.getRequest().getClientInstance().getId(),
				response.getRequestId(),
				PermissionsManager.get().getUserName()));
		List<DomainTransformException> transformExceptions = response
				.getTransformExceptions();
		for (DomainTransformException ex : transformExceptions) {
			logger.warn("Per-event error: " + ex.getMessage());
			if (ex.getEvent() != null) {
				logger.warn("Event: " + ex.getEvent().toDebugString());
			}
		}
		File file = DataFolderProvider.get()
				.getChildFile(Ax.format("dtr-exception/%s.txt", LocalDateTime
						.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
		file.getParentFile().mkdirs();
		ResourceUtilities.write(response.getRequest().toString(), file);
		logger.warn(
				Ax.format("Request with exceptions written to: \n\t%s", file));
	}

	protected DomainTransformLayerWrapper doPersistTransforms(String tag,
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
				DomainTransformLayerWrapper wrapper = get()
						.transformFromServletLayer(tag);
				// see preamble to cascading transform support
				while (cascadingTransformSupport.hasChildren()) {
					logger.debug(
							"Servlet layer - waiting for cascading transforms");
					synchronized (cascadingTransformSupport) {
						if (cascadingTransformSupport.hasChildren()) {
							cascadingTransformSupport.wait(1000);
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
		} catch (RuntimeException re) {
			re.printStackTrace();
			ThreadlocalTransformManager.cast().resetTltm(null);
			Transaction.current().toDbAborted();
			Transaction.endAndBeginNew();
			throw re;
		} finally {
			tpm.popUser();
			Preconditions.checkState(
					TransformManager.get().getTransforms().size() == 0);
			MetricLogging.get().setMuted(muted);
		}
	}

	protected DomainTransformLayerWrapper submitAndHandleTransforms(
			TransformPersistenceToken persistenceToken)
			throws DomainTransformRequestException {
		List<TransformPersistenceToken> perStoreTokens = persistenceToken
				.toPerStoreTokens();
		if (perStoreTokens.size() == 1) {
			TransformPersistenceToken perStoreToken = perStoreTokens.get(0);
			if (perStoreToken.provideTargetsWritableStore()) {
				return submitAndHandleTransformsWritableStore(perStoreToken);
			}
		}
		TransformPersistenceToken targetingWriteableStore = perStoreTokens
				.stream().filter(token -> token.provideTargetsWritableStore())
				.findFirst().orElse(null);
		DomainTransformLayerWrapper result = new DomainTransformLayerWrapper(
				targetingWriteableStore);
		for (TransformPersistenceToken perStoreToken : perStoreTokens) {
			if (perStoreToken.provideTargetsWritableStore()) {
				result.merge(
						submitAndHandleTransformsWritableStore(perStoreToken));
			} else {
				DomainTransformLayerWrapper remoteWrapperResult = Registry
						.impl(RemoteTransformPersister.class,
								perStoreToken.getTargetStore()
										.getDomainDescriptor().getClass())
						.submitAndHandleTransformsRemoteStore(perStoreToken);
				result.merge(remoteWrapperResult);
			}
		}
		return result;
	}

	protected DomainTransformLayerWrapper
			submitAndHandleTransformsWritableStore(
					TransformPersistenceToken persistenceToken)
					throws DomainTransformRequestException {
		boolean unexpectedException = true;
		try {
			LooseContext.push();
			LooseContext.remove(
					ThreadlocalTransformManager.CONTEXT_THROW_ON_RESET_TLTM);
			if (!PermissionsManager.get().isRoot()) {
				AppPersistenceBase.checkNotReadOnly();
			}
			DomainStore.stores().writableStore().getPersistenceEvents()
					.fireDomainTransformPersistenceEvent(
							new DomainTransformPersistenceEvent(
									persistenceToken, null,
									DomainTransformPersistenceEventType.PRE_COMMIT,
									true));
			MetricLogging.get().start("transform-commit");
			Transaction.current().toDbPersisting();
			DomainTransformLayerWrapper wrapper = Registry
					.impl(TransformPersistenceQueue.class)
					.submit(persistenceToken);
			// Date transactionCommitTime = wrapper.persistentRequests.get(0)
			// .getTransactionCommitTime();
			// transactionCommitTime probably not set yet, so use system date
			// (this timestamp is just advisory - the important one is in
			// transaction phase TO_DOMAIN, not
			// this one)
			//
			// also, we may have no persistentrequests (if the request has
			// already been committed)
			ThreadlocalTransformManager.cast().resetTltm(null);
			if (wrapper.response
					.getResult() == DomainTransformResponseResult.OK) {
				Transaction.current().toDbPersisted(
						new Timestamp(System.currentTimeMillis()));
			} else {
				Transaction.current().toDbAborted();
			}
			MetricLogging.get().end("transform-commit");
			handleWrapperTransforms();
			wrapper.ignored = persistenceToken.ignored;
			DomainTransformPersistenceEvent event = new DomainTransformPersistenceEvent(
					persistenceToken, wrapper,
					wrapper.providePersistenceEventType(), true);
			event.setFiringFromQueue(wrapper.fireAsQueueEvent);
			CascadingTransformSupport.register(event);
			DomainStore.stores().writableStore().getPersistenceEvents()
					.fireDomainTransformPersistenceEvent(event);
			unexpectedException = false;
			if (wrapper.response
					.getResult() == DomainTransformResponseResult.OK) {
				wrapper.response.setLogOffset(wrapper.getLogOffset());
				return wrapper;
			} else {
				logTransformException(wrapper.response);
				throw new DomainTransformRequestException(wrapper.response);
			}
		} finally {
			if (unexpectedException) {
				try {
					topicUnexpectedExceptionBeforePostTransform()
							.publish(persistenceToken);
				} catch (Throwable t) {
					// make sure we get out alive
					t.printStackTrace();
				}
			}
			LooseContext.getContext().pop();
		}
	}

	public static class ReuseIUserHolder {
		public IUser iUser;
	}

	// FIXME - sessioncontext - this should all probably go there
	private static class CommitClientInstanceContext {
		private long clientInstanceId;

		private String committerIpAddress;

		private long userId;

		public CommitClientInstanceContext(long clientInstanceId, long userId,
				String committerIpAddress) {
			this.clientInstanceId = clientInstanceId;
			this.userId = userId;
			this.committerIpAddress = committerIpAddress;
		}
	}

	static class IsWrappedObjectDteFilter
			implements CollectionFilter<DomainTransformEvent> {
		Class clazz = PersistentImpl.getImplementation(WrappedObject.class);

		@Override
		public boolean allow(DomainTransformEvent o) {
			return o.getObjectClass() == clazz;
		}
	}
}

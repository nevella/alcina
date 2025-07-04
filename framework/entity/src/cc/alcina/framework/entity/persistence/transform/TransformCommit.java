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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
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
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DeltaApplicationRecordSerializerImpl;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.permissions.Permissions.LoginState;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.ServerClientInstance;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.domain.LockUtils;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.AdjunctTransformCollation;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.EntityLocatorMap;
import cc.alcina.framework.entity.transform.RemoteTransformPersister;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.TransformConflicts;
import cc.alcina.framework.entity.transform.TransformConflicts.TransformConflictsFromOfflineSupport;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvents;
import cc.alcina.framework.entity.transform.policy.PersistenceLayerTransformRetryPolicy;
import cc.alcina.framework.entity.transform.policy.TransformPropagationPolicy;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.entity.util.ProcessLogFolder;
import cc.alcina.framework.gwt.persistence.client.DTESerializationPolicy;

/**
 *
 */
@Registration.Singleton
public class TransformCommit {
	private static final String OFFLINE_TRANSFORMS_PARTIAL = "offlineTransforms-partial";

	private static final String DTR_EXCEPTION = "dtr-exception";

	public static final transient String CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH = TransformCommit.class
			.getName() + ".CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH";

	public static final transient String CONTEXT_COMMIT_WITH_BACKOFF = TransformCommit.class
			.getName() + ".CONTEXT_COMMIT_WITH_BACKOFF";

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

	public static final transient String CONTEXT_COMMITTING = TransformCommit.class
			.getName() + ".CONTEXT_COMMITTING";

	public static final LooseContext.Key<PersistenceLayerTransformRetryPolicy> CONTEXT_RETRY_POLICY = LooseContext
			.key(TransformCommit.class, "CONTEXT_RETRY_POLICY");

	static Logger logger = LoggerFactory.getLogger(TransformCommit.class);

	public static final Topic<TransformPersistenceToken> topicUnexpectedExceptionBeforePostTransform = Topic
			.create();

	public static int commitBulkTransforms(List<DeltaApplicationRecord> records,
			Boolean useWrapperUser, boolean throwPersistenceExceptions)
			throws WebException {
		CommonPersistenceLocal cp = Registry
				.impl(CommonPersistenceProvider.class).getCommonPersistence();
		boolean persistAsOneTransaction = Configuration
				.is("persistOfflineTransformsAsOneTransaction");
		try {
			// save a copy of the records
			{
				String folderName = Ax.format("cli_%s_time_%s",
						records.get(0).getClientInstanceId(),
						DateStyle.TIMESTAMP.format(new Date()));
				File offlineDir = DataFolderProvider.get()
						.getChildFile(OFFLINE_TRANSFORMS_PARTIAL);
				File saveDir = FileUtils.child(offlineDir, folderName);
				saveDir.mkdirs();
				DeltaApplicationRecordSerializerImpl recordSerializer = new DeltaApplicationRecordSerializerImpl();
				for (DeltaApplicationRecord record : records) {
					int id = record.getRequestId();
					long clientInstanceId = record.getClientInstanceId();
					String fileName = String.format("%s_%s_ser.txt",
							clientInstanceId, id);
					File out = FileUtils.child(saveDir, fileName);
					Io.write().string(recordSerializer.write(record))
							.toFile(out);
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
					logger.warn("Request [{}/{}] already written", requestId,
							clientInstanceId);
					continue;
				}
				String chunkUuidString = deltaRecord.getChunkUuidString();
				DomainTransformRequest request = DomainTransformRequest
						.fromString(deltaRecord.getText(), chunkUuidString);
				ClientInstance clientInstance = null;
				if (deltaRecord.getClientInstanceId() == ClientInstance
						.current().getId()) {
					clientInstance = ClientInstance.current();
				} else {
					clientInstance = clientInstanceClass
							.getDeclaredConstructor().newInstance();
					clientInstance.setAuth(deltaRecord.getClientInstanceAuth());
					clientInstance.setId(deltaRecord.getClientInstanceId());
				}
				request.setClientInstance(clientInstance);
				if (chunkUuidString == null) {
					// incoming 'submit transforms' - create uuid here
					DomainTransformRequest persistableRequest = DomainTransformRequest
							.createPersistableRequest(requestId,
									request.getClientInstance().getId());
					// just hijack the uuid
					request.setChunkUuidString(
							persistableRequest.getChunkUuidString());
				}
				boolean committingUserIsAdministrator = UserlandProvider.get()
						.getGroupByName(Permissions.Names.ADMINISTRATORS_GROUP)
						.containsUserOrMemberGroupContainsUser(UserlandProvider
								.get().getUserById(deltaRecord.getUserId()));
				if (useWrapperUser == null) {
					useWrapperUser = committingUserIsAdministrator
							&& LooseContext.getContext().getBoolean(
									TransformCommit.CONTEXT_USE_WRAPPER_USER_WHEN_PERSISTING_OFFLINE_TRANSFORMS)
							&& deltaRecord.getUserId() != Permissions.get()
									.getUserId();
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
						if (!Permissions.get().isAdmin()) {
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
							wrapperUser = (IUser) Domain.find(
									(Class) PersistentImpl
											.getImplementation(IUser.class),
									deltaRecord.getUserId());
							if (reuseIUserHolder != null) {
								reuseIUserHolder.iUser = wrapperUser;
							}
						}
						if (wrapperUser == null) {
							// admin persistence
							wrapperUser = Permissions.get().getUser();
						}
						boolean asRoot = wrapperUser == UserlandProvider.get()
								.getSystemUser();
						Permissions.pushUser(wrapperUser, LoginState.LOGGED_IN,
								asRoot, clientInstance);
					} else {
						if (!Objects.equals(
								request.getClientInstance().domain()
										.domainVersion().provideUser(),
								Permissions.get().getUser())) {
							throw new UnsupportedOperationException(
									"May need to create an additional authenticationSession");
							// request.getClientInstance()
							// .setUser(Permissions.get().getUser());
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
						Permissions.popContext();
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
		boolean commitAsWrapperUser = Configuration.is("commitAsWrapperUser");
		boolean committingVmLocalRecord = dar
				.getClientInstanceId() == ClientInstance.current().getId();
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
				commitBulkTransforms(List.of(chunk), commitAsWrapperUser, true);
				String message = String.format(
						"written chunk - writing chunk %s of %s", range, size);
				System.out.println(message);
				idx = range.i2;
			}
		} else {
			dar.setType(DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED);
			commitBulkTransforms(List.of(dar), commitAsWrapperUser, true);
		}
	}

	public static void
			commitLocalTransformsInChunks(int maxTransformChunkSize) {
		try {
			Permissions.runThrowingWithPushedSystemUserIfNeeded(() -> get()
					.commitLocalTranformInChunks0(maxTransformChunkSize));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			ThreadlocalTransformManager.cast().resetTltm(null);
		}
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
			throw WrappedRuntimeException.wrap(
					layerWrapper.response.getTransformExceptions().get(0));
		}
		return pendingTransformCount;
	}

	public static DomainTransformLayerWrapper commitTransforms(String tag,
			boolean asRoot, boolean returnResponse) {
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
		if (Ax.isTest() && ServerClientInstance.get() == null) {
			// pre-login test tx (say fixing up credentials) - create dummy
			ThreadlocalTransformManager.cast().resetTltm(null);
			return new DomainTransformLayerWrapper(null);
		}
		int maxTransformChunkSize = Configuration
				.getInt("maxTransformChunkSize");
		/*
		 * If context not set (by http request), it's from the server
		 */
		LooseContext.ensure(CONTEXT_COMMIT_CLIENT_INSTANCE_CONTEXT,
				() -> new CommitClientInstanceContext(
						ServerClientInstance.get().getId(),
						Permissions.get().getUserId(), "0.0.0.0"));
		long persistentTransformRecordCount = TransformPropagationPolicy.get()
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
		if (LooseContext.is(CONTEXT_COMMIT_WITH_BACKOFF)) {
			return commitWithBackoff();
		} else {
			return commitTransforms(true);
		}
	}

	/*
	 * FIXME - mvcc.jobs - this behaviour (with backoff) is generally incorrect,
	 * // since it essentially just squelches OptimisticLockExceptions - which
	 * are // key to (say) preventing concurrent writes to job objects, which in
	 * turn break the JobAllocator contracts.
	 *
	 * So...selectively remove and look at rethrowing with some sort of wrapping
	 * (checked) concurrency exception and handling higher in the calling stack
	 * - i.e. move from commitWithBackoff (no checked) to
	 * commitWithConcurrencyCheck (checked)
	 * 
	 * Update - in the specific case of job update exceptions, backoff only
	 * occurs on non-status updates - so the
	 * PersistenceLayerTransformRetryPolicy.JobPersistenceBackoff is
	 * appropriate, and this (more restricted than was true when the preceding 2
	 * paras were written) code is ok
	 * 
	 * More generally - transformcommit wants an api rewrite - but this is
	 * actually (because restricted) now OK, I think
	 *
	 */
	public static int commitWithBackoff() {
		return commitWithBackoff(0, 8, 40, 2.0);
	}

	public static int commitWithBackoff(int initialDelayMs, int retries,
			double delayMs, double retryMultiplier) {
		try {
			LooseContext.push();
			try {
				Thread.sleep(initialDelayMs);
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
			CONTEXT_RETRY_POLICY.set(
					new PersistenceLayerTransformRetryPolicy.JobPersistenceBackoff(
							initialDelayMs, retries, delayMs, retryMultiplier));
			return commitTransforms(true);
		} finally {
			LooseContext.pop();
		}
	}

	public static int enqueueTransforms(String transformQueueName,
			Class<? extends Entity>... entityClassNames) {
		List<DomainTransformEvent> transforms = removeTransforms(
				entityClassNames);
		return BackendTransformQueue.get().enqueue(transforms,
				transformQueueName);
	}

	public static TransformCommit get() {
		return Registry.impl(TransformCommit.class);
	}

	public static boolean isCommitTestTransforms() {
		return Configuration.is("commitTestTransforms");
	}

	public static boolean isCommitting() {
		return LooseContext.is(CONTEXT_COMMITTING);
	}

	public static boolean isTestTransformCascade() {
		return Configuration.is("testTransformCascade");
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

	private Map<Long, EntityLocatorMap> clientInstanceLocatorMap = new ConcurrentHashMap<>();

	private AtomicInteger transformRequestCounter = new AtomicInteger(0);

	private void commitLocalTranformInChunks0(int maxTransformChunkSize)
			throws Exception {
		Transaction.current().clearLocalEvictionList();
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
		if (fromInstance == ServerClientInstance.get()) {
			// this jvm controls the requestId counter, so just pass the next
			// local request id and the local client instance
			commitInstance = ClientInstance.current();
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

	protected DomainTransformLayerWrapper doPersistTransforms(String tag,
			boolean asRoot) {
		// for debugging
		Set<DomainTransformEvent> transforms = TransformManager.get()
				.getTransforms();
		boolean muted = MetricLogging.get().isMuted();
		try {
			MetricLogging.get().setMuted(true);
			if (asRoot) {
				Permissions.pushSystemUser();
			} else {
				Permissions.pushCurrentUser();
			}
			return get().transformFromServletLayer(tag);
		} catch (Exception ex) {
			ex.printStackTrace();
			ThreadlocalTransformManager.cast().resetTltm(null);
			Transaction.current().toDbAborted();
			Transaction.endAndBeginNew();
			throw WrappedRuntimeException.wrap(ex);
		} finally {
			Permissions.popContext();
			Preconditions.checkState(
					TransformManager.get().getTransforms().size() == 0);
			MetricLogging.get().setMuted(muted);
		}
	}

	public void enqueueBackendTransform(Runnable runnable) {
		enqueueBackendTransform(runnable, null);
	}

	public void enqueueBackendTransform(Runnable runnable, String queueName) {
		BackendTransformQueue.get().enqueue(runnable, queueName);
	}

	public EntityLocatorMap getLocatorMapForClient(
			ClientInstance clientInstance, boolean forceRefresh) {
		if (clientInstance == ServerClientInstance.get()) {
			return EntityLayerObjects.get()
					.getServerAsClientInstanceEntityLocatorMap();
		}
		Long clientInstanceId = clientInstance.getId();
		if (!clientInstanceLocatorMap.containsKey(clientInstanceId)
				|| forceRefresh) {
			/*
			 * Note that synchronisation in DomainStore.ensureEntity means there
			 * will only be one visible clientInstance with id x *at any one
			 * time*, but that doesn't mean the clientInstance object instance
			 * won't change (since the object is potentially lazy loaded and
			 * thus vacuumed). So - to avoid confusion - use the id (guaranteed
			 * non-zero)
			 *
			 */
			// synchronized (clientInstance) {
			synchronized (LockUtils.obtainClassIdLock(clientInstance)) {
				if (!clientInstanceLocatorMap.containsKey(clientInstanceId)
						|| forceRefresh) {
					EntityLocatorMap locatorMap = CommonPersistenceProvider
							.get().getCommonPersistence()
							.getLocatorMap(clientInstanceId);
					clientInstanceLocatorMap.put(clientInstanceId, locatorMap);
				}
			}
		}
		EntityLocatorMap locatorMap = clientInstanceLocatorMap
				.get(clientInstanceId);
		return locatorMap;
	}

	public EntityLocatorMap
			getLocatorMapForClient(DomainTransformRequest request) {
		return getLocatorMapForClient(
				request.getClientInstance().domain().domainVersion(), false);
	}

	private void logTransformException(DomainTransformResponse response) {
		logger.warn(String.format(
				"domain transform problem - clientInstance: %s - rqId: %s - user ",
				response.getRequest().getClientInstance().getId(),
				response.getRequestId(), Permissions.get().getUserName()));
		List<DomainTransformException> transformExceptions = response
				.getTransformExceptions();
		for (DomainTransformException ex : transformExceptions) {
			logger.warn("Per-event error: " + ex.getMessage());
			if (ex.getEvent() != null) {
				logger.warn("Event: " + ex.getEvent().toDebugString());
			}
		}
		File file = DataFolderProvider.get().getChildFile(
				Ax.format("%s/%s.txt", DTR_EXCEPTION, LocalDateTime.now()
						.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
		file.getParentFile().mkdirs();
		Io.write().string(response.getRequest().toString()).toFile(file);
		logger.warn(
				Ax.format("Request with exceptions written to: \n\t%s", file));
	}

	public int nextTransformRequestId() {
		return transformRequestCounter.incrementAndGet();
	}

	public void setBackendTransformQueueMaxDelay(String queueName,
			long delayMs) {
		BackendTransformQueue.get().createBackendQueue(queueName, delayMs);
	}

	protected DomainTransformLayerWrapper submitAndHandleTransforms(
			TransformPersistenceToken persistenceToken)
			throws DomainTransformRequestException {
		Preconditions.checkState(!LooseContext.is(CONTEXT_COMMITTING),
				"Already in commit section");
		Preconditions.checkState(
				!LooseContext.is(
						DomainTransformPersistenceEvents.CONTEXT_FIRING_EVENT),
				"Cannot commit during event publication");
		try {
			LooseContext.pushWithTrue(CONTEXT_COMMITTING);
			List<TransformPersistenceToken> perStoreTokens = persistenceToken
					.toPerStoreTokens();
			if (perStoreTokens.size() == 1) {
				TransformPersistenceToken perStoreToken = perStoreTokens.get(0);
				if (perStoreToken.provideTargetsWritableStore()) {
					return submitAndHandleTransformsWritableStore(
							perStoreToken);
				}
			}
			TransformPersistenceToken targetingWriteableStore = perStoreTokens
					.stream()
					.filter(token -> token.provideTargetsWritableStore())
					.findFirst().orElse(null);
			DomainTransformLayerWrapper result = new DomainTransformLayerWrapper(
					targetingWriteableStore);
			for (TransformPersistenceToken perStoreToken : perStoreTokens) {
				if (perStoreToken.provideTargetsWritableStore()) {
					result.merge(submitAndHandleTransformsWritableStore(
							perStoreToken));
				} else {
					DomainTransformLayerWrapper remoteWrapperResult = Registry
							.query(RemoteTransformPersister.class)
							.addKeys(perStoreToken.getTargetStore()
									.getDomainDescriptor().getClass())
							.impl().submitAndHandleTransformsRemoteStore(
									perStoreToken);
					result.merge(remoteWrapperResult);
				}
			}
			return result;
		} finally {
			LooseContext.pop();
		}
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
			if (!Permissions.isRoot()) {
				AppPersistenceBase.checkNotReadOnly();
			}
			persistenceToken.getTransformCollation().refreshFromRequest();
			persistenceToken.getTransformCollation()
					.removeNonPersistentTransforms();
			DomainStore.stores().writableStore().getPersistenceEvents()
					.fireDomainTransformPersistenceEvent(
							new DomainTransformPersistenceEvent(
									persistenceToken, null,
									DomainTransformPersistenceEventType.PREPARE_COMMIT,
									true));
			persistenceToken.getTransformCollation().refreshFromRequest();
			persistenceToken.getTransformCollation()
					.removeNonPersistentTransforms();
			if (persistenceToken.isRequestorExternalToThisJvm()) {
				/*
				 * FUTURE - Check if this can be removed (yes, the 'persisted'
				 * state is different if the transform call is from a client
				 * rather than the local jvm, but should it be? The cancelled
				 * eviction/persistence checks are harmless, but it'd be good to
				 * explain why they're needed)
				 */
				Transaction.current().clearLocalEvictionList();
			}
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
			wrapper.ignored = persistenceToken.ignored;
			DomainTransformPersistenceEvent event = new DomainTransformPersistenceEvent(
					persistenceToken, wrapper,
					wrapper.providePersistenceEventType(), true);
			event.setFiringFromQueue(wrapper.fireAsQueueEvent);
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
					topicUnexpectedExceptionBeforePostTransform
							.publish(persistenceToken);
				} catch (Throwable t) {
					// make sure we get out alive
					t.printStackTrace();
				}
			}
			LooseContext.getContext().pop();
		}
	}

	/**
	 * synchronizing implies serialized transforms per clientInstance
	 */
	public DomainTransformLayerWrapper transform(DomainTransformRequest request,
			boolean ignoreClientAuthMismatch, boolean forOfflineTransforms,
			boolean blockUntilAllListenersNotified)
			throws DomainTransformRequestException {
		EntityLocatorMap locatorMap = getLocatorMapForClient(request);
		try {
			ExternalTransformLocks.get().lock(true,
					request.getClientInstance());
			synchronized (locatorMap) {
				Transaction.endAndBeginNew();
				TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
						request, locatorMap,
						request.getClientInstance() != ServerClientInstance
								.get(),
						ignoreClientAuthMismatch, forOfflineTransforms, logger,
						blockUntilAllListenersNotified);
				boolean hasUnprocessedRequests = CommonPersistenceProvider.get()
						.getCommonPersistence()
						.removeProcessedRequests(persistenceToken);
				if (hasUnprocessedRequests) {
					DomainTransformLayerWrapper result = submitAndHandleTransforms(
							persistenceToken);
					if (result.response == null) {
						Preconditions.checkState(request.getEvents().isEmpty());
						result.response = new DomainTransformResponse();
						// empty request, empty response
						result.response
								.setResult(DomainTransformResponseResult.OK);
					}
					return result;
				} else {
					/*
					 * perfectly plausible, if request [5, prior=[4]] arrives
					 * before request [4] - which depends on the network
					 */
					DomainTransformLayerWrapper result = new DomainTransformLayerWrapper();
					result.response = new DomainTransformResponse();
					result.response.setResult(DomainTransformResponseResult.OK);
					logger.info("Request {} - {} already processed",
							request.toStringForError(),
							request.getChunkUuidString());
					return result;
				}
			}
		} finally {
			ExternalTransformLocks.get().lock(false,
					request.getClientInstance());
		}
	}

	public DomainTransformLayerWrapper transformFromServletLayer(
			Collection<DomainTransformEvent> transforms, String tag)
			throws DomainTransformRequestException {
		int requestId = nextTransformRequestId();
		EntityLocatorMap map = EntityLayerObjects.get()
				.getServerAsClientInstanceEntityLocatorMap();
		ClientInstance clientInstance = ServerClientInstance.get();
		DomainTransformRequest request = DomainTransformRequest
				.createPersistableRequest(requestId, clientInstance.getId());
		request.setClientInstance(clientInstance);
		if (tag == null && Configuration.is("tagTransformsWithThreadName")) {
			tag = Thread.currentThread().getName();
		}
		if (tag == null) {
			tag = DomainTransformRequestTagProvider.get().getTag();
		}
		request.setTag(tag);
		request.setRequestId(requestId);
		for (DomainTransformEvent dte : transforms) {
			dte.setCommitType(CommitType.TO_STORAGE);
		}
		request.getEvents().addAll(transforms);
		try {
			Permissions.pushSystemUser();
			TransformPersistenceToken persistenceToken = new TransformPersistenceToken(
					request, map, false, false, false, logger, true);
			CONTEXT_RETRY_POLICY.optional()
					.ifPresent(persistenceToken::setTransformRetryPolicy);
			CommitClientInstanceContext clientInstanceContext = LooseContext
					.get(CONTEXT_COMMIT_CLIENT_INSTANCE_CONTEXT);
			persistenceToken.setOriginatingUserId(clientInstanceContext.userId);
			return submitAndHandleTransforms(persistenceToken);
		} finally {
			Permissions.popContext();
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
		TransformManager.get().clearTransforms();
		return transformFromServletLayer(items, tag);
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

	@Registration.Singleton
	public static class ExternalTransformLocks {
		public static TransformCommit.ExternalTransformLocks get() {
			return Registry.impl(TransformCommit.ExternalTransformLocks.class);
		}

		public void lock(boolean lock, ClientInstance clientInstance) {
			// NOOP - always succeed
		}
	}

	public static class ProcessLogFolder_Dtr_Exception
			extends ProcessLogFolder {
		@Override
		public String getFolder() {
			return DTR_EXCEPTION;
		}
	}

	public static class ProcessLogFolder_Offline_Transforms_Partial
			extends ProcessLogFolder {
		@Override
		public String getFolder() {
			return OFFLINE_TRANSFORMS_PARTIAL;
		}
	}

	public static class ReuseIUserHolder {
		public IUser iUser;
	}

	public static void setCommitTestTransforms(boolean b) {
		Configuration.properties.set(TransformCommit.class,
				"commitTestTransforms", String.valueOf(false));
	}
}

package cc.alcina.framework.entity.persistence.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.JobRelation;
import cc.alcina.framework.common.client.logic.domaintransform.AuthenticationSession;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.Iid;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.permissions.ReadOnlyException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.transform.TransformPersister.TransformPersisterToken;
import cc.alcina.framework.entity.transform.DomainTransformEventPersistent;
import cc.alcina.framework.entity.transform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.transform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.transform.TransformPersistenceToken;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEvent;
import cc.alcina.framework.entity.transform.event.DomainTransformPersistenceEventType;
import cc.alcina.framework.common.client.logic.reflection.Registration;

public class ReadonlyInMemoryPersister {

    private static SequentialIdGenerator idGenerator = new SequentialIdGenerator();

    @SuppressWarnings("unused")
    private TransformPersisterToken persisterToken;

    private TransformPersistenceToken token;

    private DomainTransformLayerWrapper wrapper;

    private Map<Long, Long> inMemoryPersistentIds = new LinkedHashMap<>();

    private Date startPersistTime;

    Logger logger = LoggerFactory.getLogger(getClass());

    List<Class> inMemoryPersistable = new ArrayList<>();

    private ThreadlocalTransformManager tltm;

    public DomainTransformLayerWrapper commitInMemoryTransforms(TransformPersisterToken persisterToken, TransformPersistenceToken token, DomainTransformLayerWrapper wrapper) {
        Registry.impl(InMemoryPersistableProvider.class).permittedClasses().map(c -> {
            Class impl = PersistentImpl.getImplementationNonGeneric(c);
            return impl != null && impl != Void.class ? impl : c;
        }).forEach(inMemoryPersistable::add);
        this.persisterToken = persisterToken;
        this.token = token;
        this.wrapper = wrapper;
        this.startPersistTime = new Date();
        validateRequests();
        generatePersistentResponse();
        generateResponse();
        wrapper.fireAsQueueEvent = true;
        return wrapper;
    }

    private void generatePersistentResponse() {
        Class<? extends DomainTransformEventPersistent> persistentEventClass = PersistentImpl.getImplementation(DomainTransformEventPersistent.class);
        AtomicBoolean missingClassRefWarned = new AtomicBoolean();
        token.getRequest().allTransforms().forEach(transform -> {
            if (transform.getTransformType() == TransformType.CREATE_OBJECT) {
                long id = idGenerator.decrementAndGet();
                inMemoryPersistentIds.put(transform.getObjectLocalId(), id);
            }
            if (transform.getObjectId() == 0 && transform.getObjectLocalId() != 0) {
                Long id = inMemoryPersistentIds.get(transform.getObjectLocalId());
                transform.setObjectId(id);
            }
            if (transform.getValueId() == 0 && transform.getValueLocalId() != 0) {
                Long id = inMemoryPersistentIds.get(transform.getValueLocalId());
                transform.setValueId(id);
            }
        });
        for (DomainTransformRequest subRequest : token.getRequest().allRequests()) {
            subRequest.updateTransformCommitType(CommitType.ALL_COMMITTED, false);
            DomainTransformRequestPersistent persistentRequest = PersistentImpl.getNewImplementationInstance(DomainTransformRequestPersistent.class);
            persistentRequest.setId(idGenerator.decrementAndGet());
            persistentRequest.setStartPersistTime(startPersistTime);
            List<DomainTransformEvent> events = subRequest.getEvents();
            subRequest.setEvents(null);
            persistentRequest.wrap(subRequest);
            persistentRequest.setEvents(new ArrayList<DomainTransformEvent>());
            subRequest.setEvents(events);
            persistentRequest.setClientInstance(token.getRequest().getClientInstance());
            persistentRequest.setOriginatingUserId(token.getOriginatingUserId());
            wrapper.persistentRequests.add(persistentRequest);
            tltm = ThreadlocalTransformManager.cast();
            new PersistentEventPopulator().populate(null, wrapper.persistentEvents, tltm, events, token.getTransformPropagationPolicy(), persistentEventClass, persistentRequest, missingClassRefWarned, true, true);
        }
    }

    private void generateResponse() {
        DomainTransformResponse response = new DomainTransformResponse();
        response.getEventsToUseForClientUpdate().addAll(token.getClientUpdateEvents());
        response.getEventsToUseForClientUpdate().addAll(tltm.getModificationEvents());
        response.setRequestId(token.getRequest().getRequestId());
        response.setTransformsProcessed(token.getRequest().allTransforms().size());
        wrapper.response = response;
        DomainStore.writableStore().getPersistenceEvents().fireDomainTransformPersistenceEvent(new DomainTransformPersistenceEvent(token, wrapper, DomainTransformPersistenceEventType.PRE_FLUSH, true));
    }

    private boolean notInMemoryPersistable(DomainTransformEvent transform) {
        Class clazz = transform.getObjectClass();
        for (Class test : inMemoryPersistable) {
            if (test.isAssignableFrom(clazz)) {
                return false;
            }
        }
        return true;
    }

    private void validateRequests() {
        List<DomainTransformEvent> invalid = token.getRequest().allTransforms().stream().filter(this::notInMemoryPersistable).collect(Collectors.toList());
        if (invalid.size() > 0) {
            logger.warn("Invalid request - {} invalid transforms - first invalid:\n{}", invalid.size(), invalid.get(0));
            throw new ReadOnlyException();
        }
    }

    @RegistryLocation(registryPoint = InMemoryPersistableProvider.class, implementationType = ImplementationType.INSTANCE)
    @Registration(InMemoryPersistableProvider.class)
    public static class InMemoryPersistableProvider {

        public Stream<Class> permittedClasses() {
            return ((List<Class>) (List) Arrays.asList(ClientInstance.class, Iid.class, AuthenticationSession.class, Job.class, JobRelation.class)).stream();
        }
    }
}

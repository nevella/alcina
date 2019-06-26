package cc.alcina.framework.entity.domaintransform.event;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics;
import cc.alcina.framework.entity.entityaccess.metric.InternalMetrics.InternalMetricTypeAlcina;

public class DomainTransformPersistenceEvents {
    private List<DomainTransformPersistenceListener> listenerList = new ArrayList<DomainTransformPersistenceListener>();

    private List<DomainTransformPersistenceListener> nonThreadListenerList = new ArrayList<DomainTransformPersistenceListener>();

    private DomainTransformPersistenceQueue queue;

    DomainStore domainStore;

    public DomainTransformPersistenceEvents(DomainStore domainStore) {
        this.domainStore = domainStore;
        this.queue = new DomainTransformPersistenceQueue(this);
    }

    public void addDomainTransformPersistenceListener(
            DomainTransformPersistenceListener listener) {
        addDomainTransformPersistenceListener(listener, false);
    }

    public void addDomainTransformPersistenceListener(
            DomainTransformPersistenceListener listener,
            boolean listenOnNonThreadEvents) {
        listenerList.add(listener);
        if (listenOnNonThreadEvents) {
            nonThreadListenerList.add(listener);
        }
    }

    public void fireDomainTransformPersistenceEvent(
            DomainTransformPersistenceEvent event) {
        DomainStore.writableStore().onTransformsPersisted();
        fireDomainTransformPersistenceEvent0(event);
        event.getPostEventRunnables().forEach(Runnable::run);
    }

    public DomainTransformPersistenceQueue getQueue() {
        return this.queue;
    }

    public void removeDomainTransformPersistenceListener(
            DomainTransformPersistenceListener listener) {
        listenerList.remove(listener);
        nonThreadListenerList.remove(listener);
    }

    public void startEventQueue() {
        queue.startEventQueue();
    }

    private void fireDomainTransformPersistenceEvent0(
            DomainTransformPersistenceEvent event) {
        boolean hasRequests = event.getPersistedRequestIds() != null
                && event.getPersistedRequestIds().size() > 0;
        long firstRequestId = hasRequests
                ? event.getPersistedRequestIds().get(0)
                : 0;
        if (hasRequests && event.isLocalToVm()) {
            domainStore.getTransformSequencer().waitForLocalVmTransformEventPreFireBarrier(firstRequestId);
        }
        synchronized (this) {
            try {
                queue.logFiring(event);
                if (hasRequests) {
                    event.getPersistedRequestIds()
                            .forEach(queue::transformRequestQueued);
                }
                for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
                        listenerList)) {
                    // only fire ex-machine transforms to certain general
                    // listeners
                    if (event.isLocalToVm()
                            || nonThreadListenerList.contains(listener)) {
                        try {
                            InternalMetrics.get().startTracker(event,
                                    () -> describeEvent(event),
                                    InternalMetricTypeAlcina.service,
                                    Thread.currentThread().getName());
                            listener.onDomainTransformRequestPersistence(event);
                        } finally {
                            InternalMetrics.get().endTracker(event);
                        }
                    }
                }
            } finally {
                if (hasRequests) {
                    event.getPersistedRequestIds()
                            .forEach(queue::transformRequestFinishedFiring);
                }
                if (hasRequests && event.isLocalToVm()) {
                    domainStore.getTransformSequencer()
                            .finishedFiringLocalEvent(firstRequestId);
                }
                /*
                 * this can block if cascaded transforms were called, so run
                 * after finishedFiringLocalEvent
                 */
                queue.logFired(event);
            }
        }
    }

    String describeEvent(DomainTransformPersistenceEvent event) {
        return Ax.format("Persistence event: id: %s - %s",
                CommonUtils.first(event.getPersistedRequestIds()),
                event.getPersistenceEventType());
    }

    boolean isUseTransformDbCommitSequencing() {
        return domainStore.getDomainDescriptor()
                .isUseTransformDbCommitSequencing();
    }
}
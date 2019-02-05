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

    private synchronized void fireDomainTransformPersistenceEvent0(
            DomainTransformPersistenceEvent event) {
        try {
            queue.logFiring(event);
            if (event.getPersistedRequestIds() != null) {
                event.getPersistedRequestIds()
                        .forEach(queue::transformRequestQueuedLocal);
            }
            for (DomainTransformPersistenceListener listener : new ArrayList<DomainTransformPersistenceListener>(
                    listenerList)) {
                // only fire ex-machine transforms to certain general listeners
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
            if (event.getPersistedRequestIds() != null) {
                event.getPersistedRequestIds()
                        .forEach(queue::transformRequestPublishedLocal);
            }
            queue.logFired(event);
        }
    }

    String describeEvent(DomainTransformPersistenceEvent event) {
        return Ax.format("Persistence event: id: %s - %s",
                CommonUtils.first(event.getPersistedRequestIds()),
                event.getPersistenceEventType());
    }
}
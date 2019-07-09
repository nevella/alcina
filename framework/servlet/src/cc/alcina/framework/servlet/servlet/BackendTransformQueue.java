package cc.alcina.framework.servlet.servlet;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;
import cc.alcina.framework.servlet.Sx;

class BackendTransformQueue {
    private AtEndOfEventSeriesTimer persistTimer;

    List<Runnable> tasks = new ArrayList<>();

    Object persistMonitor = new Object();

    Logger logger = LoggerFactory.getLogger(getClass());

    public void enqueue(Runnable runnable) {
        synchronized (this) {
            tasks.add(runnable);
            persistTimer.triggerEventOccurred();
        }
    }

    private void persistQueue0() {
        synchronized (persistMonitor) {
            List<DomainTransformEvent> events = new ArrayList<>();
            List<Runnable> toCommit = null;
            synchronized (this) {
                toCommit = tasks;
                tasks = new ArrayList<>();
            }
            for (Runnable runnable : toCommit) {
                ThreadlocalTransformManager.cast().resetTltm(null);
                try {
                    LooseContext.push();
                    runnable.run();
                    events.addAll(TransformManager.get().getTransforms());
                    TransformManager.get().clearTransforms();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    LooseContext.pop();
                }
            }
            ThreadlocalTransformManager.get().addTransforms(events, false);
            if (events.size() > 0) {
                logger.warn("(Backend queue)  - committing {} transforms",
                        events.size());
            }
            Sx.commit();
        }
    }

    void appShutdown() {
        persistTimer.cancel();
        persistQueue();
    }

    void persistQueue() {
        ThreadedPermissionsManager.cast()
                .runWithPushedSystemUserIfNeeded(() -> {
                    try {
                        persistQueue0();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    void start() {
        persistTimer = new AtEndOfEventSeriesTimer<>(500, new Runnable() {
            @Override
            public void run() {
                persistQueue();
            }
        }).maxDelayFromFirstAction(500);
    }
}

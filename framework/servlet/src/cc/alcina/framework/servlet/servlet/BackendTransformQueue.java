package cc.alcina.framework.servlet.servlet;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;
import cc.alcina.framework.servlet.Sx;
import cc.alcina.framework.servlet.servlet.ServletLayerTransforms.TransformPriorityStd;

class BackendTransformQueue {
    private AtEndOfEventSeriesTimer persistTimer;

    List<Runnable> tasks = new ArrayList<>();

    Object persistMonitor = new Object();

    Logger logger = LoggerFactory.getLogger(getClass());

    public void enqueue(Runnable runnable) {
        int size = 0;
        synchronized (this) {
            tasks.add(runnable);
            size = tasks.size();
            persistTimer.triggerEventOccurred();
        }
        if (size > ResourceUtilities.getInteger(BackendTransformQueue.class,
                "maxRunnables")) {
            new Thread("backend-transform-queue-commit") {
                @Override
                public void run() {
                    try {
                        persistQueue();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                };
            }.start();
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
            try {
                LooseContext.push();
                ServletLayerTransforms
                        .setPriority(TransformPriorityStd.Backend_admin);
                Sx.commit();
            } finally {
                LooseContext.pop();
            }
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
        int loopDelay = ResourceUtilities
                .getInteger(BackendTransformQueue.class, "loopDelay");
        persistTimer = new AtEndOfEventSeriesTimer<>(loopDelay, new Runnable() {
            @Override
            public void run() {
                persistQueue();
            }
        }).maxDelayFromFirstAction(loopDelay);
    }
}

package cc.alcina.framework.servlet.servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.PermissionsManagerState;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.NamedThreadFactory;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.servlet.AlcinaParallel.Parameters.Builder;

public class AlcinaParallel {
    public static Builder builder() {
        return new Builder();
    }

    private ThreadPoolExecutor executor;

    private boolean cancelled;

    public List<Throwable> exceptions = new ArrayList<>();

    private Parameters parameters;

    private JobTracker jobTracker;

    public AlcinaParallel(Parameters parameters) {
        this.parameters = parameters;
    }

    public void cancel() {
        cancelled = true;
        executor.shutdownNow();
    }

    public AlcinaParallelResults run() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
                parameters.threadCount,
                new NamedThreadFactory(parameters.provideThreadName()));
        jobTracker = JobRegistry.get().getContextTracker();
        LooseContextInstance snapshot = LooseContext.getContext().snapshot();
        List<Callable> callables = parameters.runnables.stream()
                .map(runnable -> wrapRunnableForParallel(snapshot, runnable))
                .collect(Collectors.toList());
        try {
            executor.invokeAll((List) callables);
        } catch (InterruptedException e) {
            // oksies, cancelled
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
        executor.shutdown();
        return new AlcinaParallelResults();
    }

    Callable wrapRunnableForParallel(LooseContextInstance snapshot,
            Runnable runnable) {
        PermissionsManagerState permissionsManagerState = PermissionsManager
                .get().snapshotState();
        return () -> {
            try {
                LooseContext.push();
                if (cancelled) {
                    return null;
                }
                if (jobTracker != null && jobTracker.isCancelled()) {
                    return null;
                }
                permissionsManagerState.copyTo(PermissionsManager.get());
                LooseContext.putSnapshotProperties(snapshot);
                DomainStore.ensureActiveTransaction();
                runnable.run();
            } catch (RuntimeException e) {
                e.printStackTrace();
                if (parameters.cancelOnException) {
                    cancelled = true;
                }
                exceptions.add(e);
            } finally {
                LooseContext.pop();
                ThreadlocalTransformManager.cast().resetTltm(null);
            }
            return null;
        };
    }

    public class AlcinaParallelResults {
        public void throwOnException() {
            if (exceptions.size() > 0) {
                throw new WrappedRuntimeException(exceptions.get(0));
            }
        }
    }

    public static class Parameters {
        private boolean cancelOnException;

        private int threadCount;

        private List<Runnable> runnables;

        private String threadName;

        public Parameters() {
        }

        private Parameters(Builder builder) {
            this.cancelOnException = builder.cancelOnException;
            this.threadCount = builder.threadCount;
            this.runnables = builder.runnables;
            this.threadName = builder.threadName;
        }

        public String provideThreadName() {
            return Optional.<String> ofNullable(threadName)
                    .orElse("alcina-parallel");
        }

        public static final class Builder {
            private boolean cancelOnException;

            private int threadCount;

            private List<Runnable> runnables = Collections.emptyList();

            private String threadName;

            private Builder() {
            }

            public Parameters build() {
                return new Parameters(this);
            }

            public AlcinaParallelResults run() {
                return new AlcinaParallel(new Parameters(this)).run();
            }

            public Builder withCancelOnException(boolean cancelOnException) {
                this.cancelOnException = cancelOnException;
                return this;
            }

            public Builder withRunnables(List<Runnable> runnables) {
                this.runnables = runnables;
                return this;
            }

            public Builder withThreadCount(int threadCount) {
                this.threadCount = threadCount;
                return this;
            }

            public Builder withThreadName(String threadName) {
                this.threadName = threadName;
                return this;
            }
        }
    }
}

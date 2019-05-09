package cc.alcina.framework.servlet.servlet;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class AlcinaParallel {
    private ThreadPoolExecutor executor;

    private boolean cancelled;

    public List<Future> results;

    public AlcinaParallel(Parameters parameters) {
    }

    public AlcinaParallelResults run() {
        // executor=
        return new AlcinaParallelResults();
    }

    public class AlcinaParallelResults {
    }

    public static class Parameters {
        public static Builder builder() {
            return new Builder();
        }

        private boolean cancelOnException;

        private int threadCount;

        private boolean logExceptions = true;

        private List<Runnable> runnables;

        private String threadName;

        public Parameters() {
        }

        private Parameters(Builder builder) {
            this.cancelOnException = builder.cancelOnException;
            this.threadCount = builder.threadCount;
            this.logExceptions = builder.logExceptions;
            this.runnables = builder.runnables;
            this.threadName = builder.threadName;
        }

        public static final class Builder {
            private boolean cancelOnException;

            private int threadCount;

            private boolean logExceptions;

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

            public Builder withLogExceptions(boolean logExceptions) {
                this.logExceptions = logExceptions;
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

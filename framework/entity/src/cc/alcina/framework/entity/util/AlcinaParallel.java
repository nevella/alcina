package cc.alcina.framework.entity.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.PermissionsManagerState;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContextInstance;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.NamedThreadFactory;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;
import cc.alcina.framework.entity.util.AlcinaParallel.Parameters.Builder;

public class AlcinaParallel {
	public static Builder builder() {
		return new Builder();
	}

	private ThreadPoolExecutor executor;

	private boolean cancelled;

	public List<Throwable> exceptions = new ArrayList<>();

	private Parameters parameters;

	private AlcinaParallelJobChecker jobChecker;

	public AlcinaParallel(Parameters parameters) {
		this.parameters = parameters;
	}

	public void cancel() {
		cancelled = true;
		executor.shutdownNow();
	}

	public AlcinaParallelResults run() {
		jobChecker = Registry.impl(AlcinaParallelJobChecker.class);
		if (parameters.serial) {
			for (Runnable runnable : parameters.runnables) {
				try {
					LooseContext.push();
					if (cancelled) {
						break;
					}
					if (jobChecker.isCancelled()) {
						break;
					}
					runnable.run();
				} catch (Throwable t) {
					t.printStackTrace();
					if (parameters.cancelOnException) {
						cancelled = true;
					}
					exceptions.add(t);
				} finally {
					LooseContext.pop();
				}
			}
			return new AlcinaParallelResults();
		} else {
			executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
					parameters.threadCount,
					new NamedThreadFactory(parameters.provideThreadName()));
			LooseContextInstance snapshot = LooseContext.getContext()
					.snapshot();
			List<Callable> callables = parameters.runnables.stream().map(
					runnable -> wrapRunnableForParallel(snapshot, runnable))
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
				if (jobChecker.isCancelled()) {
					return null;
				}
				permissionsManagerState.copyTo(PermissionsManager.get());
				LooseContext.putSnapshotProperties(snapshot);
				if (DomainStore.stores().hasInitialisedDatabaseStore()) {
					DomainStore.ensureActiveTransaction();
				}
				runnable.run();
			} catch (Throwable t) {
				t.printStackTrace();
				if (parameters.cancelOnException) {
					cancelled = true;
				}
				exceptions.add(t);
			} finally {
				LooseContext.pop();
				ThreadlocalTransformManager.cast().resetTltm(null);
			}
			return null;
		};
	}

	@RegistryLocation(registryPoint = AlcinaParallelJobChecker.class, implementationType = ImplementationType.INSTANCE)
	public static class AlcinaParallelJobChecker {
		public boolean isCancelled() {
			return false;
		}
	}

	public class AlcinaParallelResults {
		public AlcinaParallel getRunner() {
			return AlcinaParallel.this;
		}

		public boolean hadExceptions() {
			return exceptions.size() > 0;
		}

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

		private boolean serial;

		public Parameters() {
		}

		private Parameters(Builder builder) {
			this.cancelOnException = builder.cancelOnException;
			this.threadCount = builder.threadCount;
			this.runnables = builder.runnables;
			this.threadName = builder.threadName;
			this.serial = builder.withSerial;
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

			private boolean withSerial;

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

			public Builder withSerial(boolean withSerial) {
				this.withSerial = withSerial;
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

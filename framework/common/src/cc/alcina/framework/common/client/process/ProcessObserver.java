package cc.alcina.framework.common.client.process;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.util.HasBind;

/**
 * Marker interface: observes AlcinaProcess observable topics
 *
 *
 *
 */
public interface ProcessObserver<T extends ProcessObservable>
		extends TopicListener<T>, HasBind {
	default void bind() {
		ProcessObservers.observe(this, true);
	}

	default Class<T> getObservableClass() {
		return Reflections.at(this).getGenericBounds().bounds.get(0);
	}

	default void unbind() {
		ProcessObservers.observe(this, false);
	}

	/**
	 * A marker that models whole app process observation. Usage: as the parent
	 * of non-VCS implementations which provide debugger hooks (for say
	 * intercepting failed test runs/steps in long test processes, or dirndl
	 * debugging)
	 *
	 *
	 *
	 */
	public abstract static class AppDebug implements HasObservers {
		public static void register() {
			Registry.query(AppDebug.class).implementations()
					.forEach(AppDebug::attach);
		}

		public void attach() {
			ProcessObservers.observe(this);
		}

		@Override
		public List<ProcessObserver> getObservers() {
			return List.of();
		}
	}

	/**
	 * A marker for jvm-only app observation (where the classpath possibly
	 * includes gwt app observers)
	 *
	 */
	@Registration(AppDebugJvm.class)
	public abstract static class AppDebugJvm implements HasObservers {
		public void attach() {
			ProcessObservers.observe(this);
		}
	}

	public interface HasObservers {
		public List<ProcessObserver> getObservers();
	}
}

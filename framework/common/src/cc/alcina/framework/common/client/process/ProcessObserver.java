package cc.alcina.framework.common.client.process;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.TopicListener;

/**
 * Marker interface: observes AlcinaProcess observable topics
 *
 * 
 *
 */
public interface ProcessObserver<T extends ProcessObservable>
		extends TopicListener<T> {
	public Class<T> getObservableClass();

	/**
	 * A marker that models whole app process observation. Usage: as the parent
	 * of non-VCS implementations which provide debugger hooks (for say
	 * intercepting failed test runs/steps in long test processes, or dirndl
	 * debugging)
	 *
	 * 
	 *
	 */
	@Registration.Singleton
	public abstract static class AppDebug implements HasObservers {
		public void attach() {
			ProcessObservers.observe(this);
		}

		@Override
		public List<ProcessObserver> getObservers() {
			return List.of();
		}
	}

	public interface HasObservers {
		public List<ProcessObserver> getObservers();
	}
}

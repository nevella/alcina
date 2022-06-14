package cc.alcina.framework.gwt.client.dirndl.behaviour;

import java.util.Map;

import cc.alcina.framework.common.client.util.CollectionCreators;

public class RequestAnimationFrameGate {
	private Map<Class, Boolean> scheduled = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	public void schedule(Runnable runnable) {
		Class<? extends Runnable> clazz = runnable.getClass();
		if (!scheduled.containsKey(clazz)) {
			scheduled.put(clazz, true);
			runInRequestAnimationFrame(runnable);
		}
	}

	private void clearScheduled(Runnable runnable) {
		Class<? extends Runnable> clazz = runnable.getClass();
		scheduled.remove(clazz);
	}

	private final native void runInRequestAnimationFrame(Runnable runnable) /*-{
    $wnd
        .requestAnimationFrame(function() {
          try {
            runnable.@java.lang.Runnable::run()();
          } finally {
            this.@cc.alcina.framework.gwt.client.dirndl.behaviour.InferredDomEvents.RequestAnimationFrameGate::clearScheduled(Ljava/lang/Runnable;)(runnable);
          }
        });

	}-*/;
}
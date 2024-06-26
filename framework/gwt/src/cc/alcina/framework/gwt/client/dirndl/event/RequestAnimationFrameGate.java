package cc.alcina.framework.gwt.client.dirndl.event;

import java.util.Map;

import cc.alcina.framework.common.client.util.CollectionCreators;

public class RequestAnimationFrameGate {
	private Map<Class, Boolean> scheduled = CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	private void clearScheduled(Runnable runnable) {
		Class<? extends Runnable> clazz = runnable.getClass();
		scheduled.remove(clazz);
	}

	private final native void runInRequestAnimationFrame(Runnable runnable) /*-{
    var $ctx = this;
    $wnd
        .requestAnimationFrame($entry(function() {
          try {
            runnable.@java.lang.Runnable::run()();
          } finally {
            $ctx.@cc.alcina.framework.gwt.client.dirndl.event.RequestAnimationFrameGate::clearScheduled(Ljava/lang/Runnable;)(runnable);
          }
        }));

	}-*/;

	public void schedule(Runnable runnable) {
		Class<? extends Runnable> clazz = runnable.getClass();
		if (!scheduled.containsKey(clazz)) {
			scheduled.put(clazz, true);
			runInRequestAnimationFrame(runnable);
		} else {
		}
	}
}
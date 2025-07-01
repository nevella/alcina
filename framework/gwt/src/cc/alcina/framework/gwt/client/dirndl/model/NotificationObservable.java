package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.ContextObservers;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.logic.LogLevel;

/**
 * <p>
 * This is implemented as a contextobservable - and that's a general pattern for
 * per-client observables.
 * 
 * <p>
 * Single-threaded (GWT) clients should register as a base observer on c
 */
@Reflected
public class NotificationObservable
		implements ContextObservers.Observable.Base {
	/**
	 * NOTE! This will return a Notification<b>Context</b>Observable if in a
	 * romcom environment
	 * 
	 * @param template
	 * @param args
	 * @return
	 */
	public static ProcessObservable of(String template, Object... args) {
		return of(Ax.format(template, args));
	}

	public static ProcessObservable of(String message) {
		NotificationObservable result = new NotificationObservable();
		result.message = message;
		return result;
	}

	public String message;

	public LogLevel level;
}
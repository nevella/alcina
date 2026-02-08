package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.ContextObservable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.LogLevel;

/**
 * <p>
 * This is implemented as a contextobservable - and that's a general pattern for
 * per-client observables.
 * 
 * <p>
 * Single-threaded (GWT) clients should register as a base observer on the
 * context
 */
@Reflected
public class NotificationObservable implements ContextObservable.Base {
	/**
	 * NOTE! This will return a Notification<b>Context</b>Observable if in a
	 * romcom environment
	 * 
	 * @param template
	 * @param args
	 * @return
	 */
	public static NotificationObservable of(String template, Object... args) {
		return of(Ax.format(template, args));
	}

	public static NotificationObservable of(String message) {
		NotificationObservable result = new NotificationObservable();
		result.message = message;
		return result;
	}

	public static NotificationObservable of(Throwable throwable) {
		NotificationObservable result = new NotificationObservable();
		result.message = CommonUtils.toSimpleExceptionMessage(throwable);
		return result;
	}

	public NotificationObservable withLevel(LogLevel level) {
		this.level = level;
		return this;
	}

	public String message;

	public LogLevel level = LogLevel.INFO;

	@Override
	public String toString() {
		return Ax.format("[%s] %s", level, message);
	}
}
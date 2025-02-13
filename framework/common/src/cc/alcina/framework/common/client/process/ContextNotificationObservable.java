package cc.alcina.framework.common.client.process;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.logic.LogLevel;

public class ContextNotificationObservable
		implements ContextObservers.Observable {
	public static ContextNotificationObservable of(String template,
			Object... args) {
		return of(Ax.format(template, args));
	}

	public static ContextNotificationObservable of(String message) {
		ContextNotificationObservable result = new ContextNotificationObservable();
		result.message = message;
		return result;
	}

	public String message;

	public LogLevel level;
}

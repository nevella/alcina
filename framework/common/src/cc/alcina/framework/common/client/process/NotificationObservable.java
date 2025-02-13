package cc.alcina.framework.common.client.process;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.logic.LogLevel;

public class NotificationObservable implements ProcessObservable {
	public static NotificationObservable of(String template, Object... args) {
		return of(Ax.format(template, args));
	}

	public static NotificationObservable of(String message) {
		NotificationObservable result = new NotificationObservable();
		result.message = message;
		return result;
	}

	public String message;

	public LogLevel level;
}
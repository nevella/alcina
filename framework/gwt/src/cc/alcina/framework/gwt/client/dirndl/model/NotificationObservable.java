package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.process.ContextObservers;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.logic.LogLevel;

public class NotificationObservable implements ProcessObservable {
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
		if (Al.isRomcom()) {
			ContextObservable result = new ContextObservable();
			result.message = message;
			return result;
		} else {
			NotificationObservable result = new NotificationObservable();
			result.message = message;
			return result;
		}
	}

	public String message;

	public LogLevel level;

	public static class ContextObservable
			implements ContextObservers.Observable {
		public String message;

		public LogLevel level;
	}
}
package cc.alcina.framework.servlet.logging;

import cc.alcina.framework.common.client.process.ProcessObservable;

/**
 * Rather than tightly coupling a UI to write flight events to disk - take it to
 * another level, use observables.
 */
public class FlightEventRecorderObservable {
	public static class MarkRecordedEvents implements ProcessObservable {
	}

	public static class PersistRecordedEvents implements ProcessObservable {
	}
}

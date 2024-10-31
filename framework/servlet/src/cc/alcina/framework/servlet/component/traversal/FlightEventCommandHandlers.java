package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.process.NotificationObservable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.servlet.component.traversal.FlightEventCommand.MarkFlightEvents;
import cc.alcina.framework.servlet.component.traversal.FlightEventCommand.PersistFlightEvents;
import cc.alcina.framework.servlet.logging.FlightEventRecorderObservable;

public interface FlightEventCommandHandlers
		extends FlightEventCommand.PersistFlightEvents.Handler,
		FlightEventCommand.MarkFlightEvents.Handler {
	@Override
	default void onMarkFlightEvents(MarkFlightEvents event) {
		new FlightEventRecorderObservable.MarkRecordedEvents().publish();
		NotificationObservable.of("Flight events marked").publish();
	}

	@Override
	default void onPersistFlightEvents(PersistFlightEvents event) {
		try {
			new FlightEventRecorderObservable.PersistRecordedEvents().publish();
			NotificationObservable
					.of("Flight events persisted to default extract folder")
					.publish();
		} catch (Exception e) {
			e.printStackTrace();
			NotificationObservable
					.of(Ax.format("Issue persisting flight events - %s",
							CommonUtils.toSimpleExceptionMessage(e)))
					.publish();
		}
	}
}

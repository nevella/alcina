package cc.alcina.framework.servlet.logging;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.servlet.LifecycleService;

@Registration.Singleton
public class FlightEventRecorder extends LifecycleService.AlsoDev
		implements ProcessObserver<FlightEvent> {
	String path;

	@Override
	public void onApplicationStartup() {
		if (!Configuration.is("enabled")) {
			return;
		}
		ProcessObservers.observe(this, true);
	}

	Map<String, String> sessionIdDateSession = new ConcurrentHashMap<>();

	@Override
	public void topicPublished(FlightEvent message) {
		String dateSessionId = sessionIdDateSession.computeIfAbsent(
				message.event.getSessionId(), sessionId -> Ax.format("%s.%s",
						Ax.timestampYmd(new Date()), sessionId));
		String folderPath = Ax.format("%s/%s", Configuration.get("path"),
				dateSessionId);
		File folder = new File(folderPath);
		if (!folder.exists()) {
			folder.mkdirs();
			Ax.out("FlightEventRecorder :: recording to %s", folderPath);
		}
		String path = Ax.format("%s/%s.json", folderPath, message.eventId);
		Io.write().asReflectiveSerialized(true).object(message).toPath(path);
	}
}

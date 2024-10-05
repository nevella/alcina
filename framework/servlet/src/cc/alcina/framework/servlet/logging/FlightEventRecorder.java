package cc.alcina.framework.servlet.logging;

import java.io.File;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.flight.FlightEventWrappable;
import cc.alcina.framework.common.client.flight.FlightEventWrappable.FlightExceptionMessage;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.servlet.LifecycleService;

@Registration.Singleton
public class FlightEventRecorder extends LifecycleService.AlsoDev
		implements ProcessObserver<FlightEvent> {
	File eventsFolder;

	String sessionId;

	public static FlightEventRecorder get() {
		return Registry.impl(FlightEventRecorder.class);
	}

	RecorderThread recorderThread;

	boolean finished;

	class RecorderThread extends Thread {
		BlockingQueue<FlightEvent> events = new LinkedBlockingDeque<>();

		RecorderThread() {
			super("flightevent-recorder");
		}

		@Override
		public void run() {
			while (!finished) {
				try {
					FlightEvent event = events.poll(1, TimeUnit.SECONDS);
					if (event != null) {
						writeMessage(event);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onApplicationStartup() {
		boolean finished = false;
		if (!Configuration.is("enabled")) {
			return;
		}
		ProcessObservers.observe(this, true);
		this.recorderThread = new RecorderThread();
		this.recorderThread.start();
	}

	@Override
	public void onApplicationShutdown() {
		finished = true;
	}

	@Override
	public synchronized void topicPublished(FlightEvent message) {
		recorderThread.events.add(message);
	}

	void writeMessage(FlightEvent message) {
		File writeTo = null;
		try {
			if (sessionId == null) {
				sessionId = message.event.getSessionId();
			}
			ensureEventsFolder();
			writeTo = FileUtils.child(eventsFolder,
					String.valueOf(message.id) + ".json");
			String serialized = ReflectiveSerializer.serialize(message);
			Io.write().string(serialized).toFile(writeTo);
		} catch (Exception e) {
			e.printStackTrace();
			if (writeTo != null) {
				try {
					FlightExceptionMessage flightExceptionMessage = new FlightEventWrappable.FlightExceptionMessage(
							message.event.getSessionId(),
							CommonUtils.getFullExceptionMessage(e));
					Io.write().asReflectiveSerialized(true)
							.object(flightExceptionMessage).toFile(writeTo);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
	}

	void ensureEventsFolder() {
		if (sessionId == null || eventsFolder != null) {
			return;
		}
		String dateSessionId = Ax.format("%s.%s", Ax.timestampYmd(new Date()),
				sessionId);
		String appId = Configuration.get("appId");
		appId = Ax.blankTo(appId, EntityLayerUtils.getLocalHostName());
		String folderPath = Ax.format("%s/flight-%s-%s",
				Configuration.get("path"), appId, dateSessionId);
		eventsFolder = new File(folderPath);
		eventsFolder.mkdirs();
		Ax.out("FlightEventRecorder :: recording to %s", folderPath);
	}

	public synchronized File rollover() {
		eventsFolder = null;
		ensureEventsFolder();
		return eventsFolder;
	}
}

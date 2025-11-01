package cc.alcina.framework.servlet.logging;

import java.io.File;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.flight.FlightEventWrappable;
import cc.alcina.framework.common.client.flight.FlightEventWrappable.FlightExceptionMessage;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.LogUtil;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.servlet.LifecycleService;
import cc.alcina.framework.servlet.logging.FlightEventRecorderObservable.MarkRecordedEvents;
import cc.alcina.framework.servlet.logging.FlightEventRecorderObservable.PersistRecordedEvents;

@Registration.Singleton
public class FlightEventRecorder extends LifecycleService.AlsoDev {
	File eventsFolder;

	String sessionId;

	public long timeNs;

	public static FlightEventRecorder get() {
		return Registry.impl(FlightEventRecorder.class);
	}

	@Reflected
	class FlightEventObserver implements ProcessObserver<FlightEvent> {
		@Override
		public synchronized void topicPublished(FlightEvent message) {
			recorderThread.events.add(message);
		}
	}

	@Reflected
	class MarkRecordedEventsObserver
			implements ProcessObserver<MarkRecordedEvents> {
		@Override
		public synchronized void topicPublished(MarkRecordedEvents message) {
			rollover();
		}
	}

	@Reflected
	class PersistRecordedEventsObserver
			implements ProcessObserver<PersistRecordedEvents> {
		@Override
		public synchronized void topicPublished(PersistRecordedEvents message) {
			Preconditions.checkState(enabled.is(),
					"Flight events not enabled!");
			copyEventsToExtractFolder();
		}
	}

	RecorderThread recorderThread;

	boolean finished;

	public int threadPriority = Thread.NORM_PRIORITY;

	public int recordedEventCount;

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

	static final Configuration.Key enabled = Configuration.key("enabled");

	@Override
	public void onApplicationStartup() {
		boolean finished = false;
		if (!enabled.is()) {
			new PersistRecordedEventsObserver().bind();
			return;
		}
		maxEvents = Configuration.getInt("maxEvents");
		new FlightEventObserver().bind();
		new MarkRecordedEventsObserver().bind();
		new PersistRecordedEventsObserver().bind();
		this.recorderThread = new RecorderThread();
		this.recorderThread.setPriority(threadPriority);
		this.recorderThread.start();
	}

	@Override
	public void onApplicationShutdown() {
		finished = true;
	}

	int maxEvents;

	int counter;

	void writeMessage(FlightEvent message) {
		long nanoTime = System.nanoTime();
		try {
			writeMessage0(message);
		} finally {
			timeNs += (System.nanoTime() - nanoTime);
			recordedEventCount++;
		}
	}

	void writeMessage0(FlightEvent message) {
		File writeTo = null;
		try {
			counter++;
			if (counter > maxEvents) {
				return;
			}
			if (counter == maxEvents) {
				throw new RuntimeException(
						"Exceeded FlightEventRecorder maxEvents");
			}
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
		String appId = Configuration.key("appId").optional()
				.map(Configuration.Key::get)
				.orElse(EntityLayerUtils.getLocalHostName());
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

	synchronized File copyEventsToExtractFolder() {
		File file = eventsFolder;
		rollover();
		try {
			File copiedTo = new File(Configuration.get("extractFolder"));
			SEUtilities.copyFile(file, copiedTo);
			LogUtil.classLogger().warn("Logged flight events to: {}", copiedTo);
			return copiedTo;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}

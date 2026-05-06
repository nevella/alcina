package cc.alcina.framework.servlet.logging;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.flight.FlightEventWrappable;
import cc.alcina.framework.common.client.flight.FlightEventWrappable.FlightExceptionMessage;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.LogUtil;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.util.FileUtils;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.servlet.LifecycleService;
import cc.alcina.framework.servlet.component.sequence.AbstractSequenceLoader;
import cc.alcina.framework.servlet.component.sequence.adapter.FlightEventSequence;
import cc.alcina.framework.servlet.logging.FlightEventRecorderObservable.MarkRecordedEvents;
import cc.alcina.framework.servlet.logging.FlightEventRecorderObservable.PersistRecordedEvents;

@Registration.Singleton
public class FlightEventRecorder extends LifecycleService.AlsoDev {
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
			rollover();
		}
	}

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

	class RecordedSession {
		File folder;

		int eventCount;

		RecordedSession(File folder) {
			this.folder = folder;
		}

		void write(FlightEvent message) {
			File writeTo = null;
			try {
				eventCount++;
				if (eventCount > maxEvents.intValue()) {
					return;
				}
				if (eventCount == maxEvents.intValue()) {
					throw new RuntimeException(
							"Exceeded FlightEventRecorder maxEvents");
				}
				// may have been cleared
				folder.mkdirs();
				writeTo = FileUtils.child(folder,
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
	}

	static final Configuration.Key enabled = Configuration.key("enabled");

	public static final Configuration.Key path = Configuration.key("path");

	static final Configuration.Key extractFolderPath = Configuration
			.key("extractFolder");

	static final Configuration.Key maxEvents = Configuration.key("maxEvents");

	public static FlightEventRecorder get() {
		return Registry.impl(FlightEventRecorder.class);
	}

	RecorderThread recorderThread;

	boolean finished;

	public int threadPriority = Thread.NORM_PRIORITY;

	Map<String, RecordedSession> sessionIdFolder = CollectionCreators.Bootstrap
			.createConcurrentStringMap();

	public synchronized void clear() {
		File eventsFolder = new File(path.get());
		eventsFolder.mkdirs();
		Arrays.stream(eventsFolder.listFiles())
				.filter(f -> f.isDirectory() && !f.isHidden())
				.forEach(f -> SEUtilities.deleteDirectory(f));
	}

	@Override
	public void onApplicationStartup() {
		boolean finished = false;
		if (!enabled.is()) {
			new PersistRecordedEventsObserver().bind();
			return;
		}
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

	/*
	 * move all non-extract folders to extractFolder
	 */
	public synchronized void rollover() {
		File extractFolder = new File(extractFolderPath.get());
		extractFolder.mkdirs();
		File eventsFolder = new File(path.get());
		eventsFolder.mkdirs();
		Arrays.stream(eventsFolder.listFiles()).filter(f -> f.isDirectory()
				&& !f.isHidden() && !Objects.equals(f, extractFolder))
				.forEach(f -> {
					File to = FileUtils.child(extractFolder, f.getName());
					f.renameTo(to);
					LogUtil.classLogger().warn("Moved flight events to: {}",
							to);
				});
	}

	synchronized void writeMessage(FlightEvent message) {
		getRecordedSession(message.event.getSessionId()).write(message);
	}

	RecordedSession getRecordedSession(String sessionId) {
		if (sessionId == null) {
			return null;
		}
		RecordedSession session = sessionIdFolder.get(sessionId);
		if (session != null) {
			return session;
		}
		String dateSessionId = Ax.format("%s.%s", Ax.timestampYmd(new Date()),
				sessionId);
		String appId = Configuration.key("appId").optional()
				.map(Configuration.Key::get)
				.orElse(EntityLayerUtils.getLocalHostName());
		String folderPath = Ax.format("%s/flight-%s-%s",
				Configuration.get("path"), appId, dateSessionId);
		File folder = new File(folderPath);
		folder.mkdirs();
		session = new RecordedSession(folder);
		sessionIdFolder.put(sessionId, session);
		Ax.out("FlightEventRecorder :: recording to %s", folderPath);
		return session;
	}

	public static InstanceQuery createInstanceQuery(String path) {
		InstanceQuery query = new InstanceQuery().withType(Sequence.class);
		query.addParameters(
				new Sequence.Loader.LoaderType().withValue(FlightPath.class));
		query.addParameters(
				new Sequence.Loader.LoaderLocation().withValue(path));
		return query;
	}

	public static class FlightPath extends AbstractSequenceLoader {
		public FlightPath() {
			super(null, null, s -> s, new FlightEventSequence());
		}

		@Override
		public boolean handlesSequenceLocation(String location) {
			return false;
		}
	}
}

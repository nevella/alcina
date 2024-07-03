package cc.alcina.extras.dev.codeservice;

import static java.nio.file.StandardWatchEventKinds.*;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.extras.dev.codeservice.CodeService.Event;
import cc.alcina.framework.common.client.util.CountingMap;

/*
 * Polls events from the queue - if empty, from the watch service
 */
class EventQueue implements Runnable {
	CodeService codeService;

	EventQueue(CodeService codeService) {
		this.codeService = codeService;
	}

	BlockingQueue<CodeService.Event> queue = new LinkedBlockingQueue<>();

	Map<Object, Event> keyEvents = new LinkedHashMap<>();

	CountingMap<Class<? extends Event>> eventHisto = new CountingMap<>();

	void add(CodeService.Event event) {
		/*
		 * execution should be guaranteed single-threaded - so synchronization
		 * unneeded - but ensuring anyway
		 */
		synchronized (queue) {
			Object key = event.key();
			if (!keyEvents.containsKey(key)) {
				keyEvents.put(key, event);
				queue.add(event);
				eventHisto.add(event.getClass());
				added++;
				lastEventSubmitted = System.currentTimeMillis();
				queue.notify();
			}
		}
	}

	void start() {
		startTime = System.currentTimeMillis();
		String threadName = "codeservice-eventqueue";
		Thread thread = new Thread(this, threadName);
		thread.setDaemon(true);
		thread.start();
	}

	int processed = 0;

	int added = 0;

	long startTime;

	Logger logger = LoggerFactory.getLogger(getClass());

	boolean finished;

	long lastEventSubmitted;

	@Override
	public void run() {
		while (!finished) {
			Event event = null;
			synchronized (queue) {
				event = queue.poll();
				if (event == null) {
					try {
						codeService.onEmptyQueue();
						pollWatchService();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (event != null) {
				keyEvents.remove(event.key(), event);
				codeService.handleEvent(event);
				if (processed++ % 100 == 0) {
					logger.info("Processed: {}/{}", processed, added);
				}
			}
		}
	}

	void pollWatchService() {
		try {
			pollWatchService0();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void pollWatchService0() throws InterruptedException {
		WatchKey watchKey = codeService.watchService.poll(1, TimeUnit.SECONDS);
		if (watchKey == null) {
			return;
		}
		Path parentDir = (Path) watchKey.watchable();
		for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
			WatchEvent.Kind<?> eventKind = watchEvent.kind();
			if (eventKind == OVERFLOW) {
				logger.error("Fatal exception - watch service overflow");
				finished = true;
				System.exit(1);
				return;
			}
			Path child = parentDir.resolve((Path) watchEvent.context());
			boolean fire = false;
			if (eventKind == ENTRY_CREATE) {
				fire = true;
			} else if (eventKind == ENTRY_MODIFY) {
				fire = true;
			} else if (eventKind == ENTRY_DELETE) {
				fire = true;
			}
			if (fire) {
				codeService.context.submitFileEvent(child.toFile());
			}
		}
		watchKey.reset();
	}
}

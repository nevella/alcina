package cc.alcina.extras.dev.codeservice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.extras.dev.codeservice.CodeService.Event;
import cc.alcina.framework.common.client.util.CountingMap;

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

	@Override
	public void run() {
		for (;;) {
			Event event = null;
			synchronized (queue) {
				event = queue.poll();
				if (event == null) {
					try {
						codeService.onEmptyQueue();
						queue.wait();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (event != null) {
				codeService.handleEvent(event);
				if (processed++ % 100 == 0) {
					logger.info("Processed: {}/{}", processed, added);
				}
			}
		}
	}
}

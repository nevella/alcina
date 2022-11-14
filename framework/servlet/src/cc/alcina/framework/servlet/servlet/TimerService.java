package cc.alcina.framework.servlet.servlet;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.ThrowingRunnable;
import cc.alcina.framework.servlet.LifecycleService;

@Registration.Singleton
public class TimerService extends LifecycleService {
	public static TimerService get() {
		return Registry.impl(TimerService.class);
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	Timer timer;

	@Override
	public void onApplicationShutdown() {
		timer.cancel();
	}

	@Override
	public void onApplicationStartup() {
		timer = new Timer("Alcina-timer-service");
	}

	public void schedule(ThrowingRunnable runnable, long delay) {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					runnable.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, delay);
	}
}

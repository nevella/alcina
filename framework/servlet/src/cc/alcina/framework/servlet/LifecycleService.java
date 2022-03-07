package cc.alcina.framework.servlet;

import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration(LifecycleService.class)
public abstract class LifecycleService {
	public LifecycleService() {
	}

	public void onApplicationShutdown() {
	}

	public void onApplicationStartup() {
	}
}

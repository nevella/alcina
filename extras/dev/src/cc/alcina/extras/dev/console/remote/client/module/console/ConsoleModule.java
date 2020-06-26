package cc.alcina.extras.dev.console.remote.client.module.console;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsoleClientUtils;
import cc.alcina.framework.gwt.client.lux.LuxModule;

public class ConsoleModule {
	private static ConsoleModule consoleModule;

	public static ConsoleModule get() {
		if (consoleModule == null) {
			consoleModule = new ConsoleModule();
		}
		return consoleModule;
	}

	public ConsoleResources resources = GWT.create(ConsoleResources.class);

	private ConsoleModule() {
		LuxModule.get().interpolateAndInject(resources.consoleStyles());
	}

	public void startConsoleActivity(AcceptsOneWidget panel,
			ConsoleActivity consoleActivity) {
		GWT.runAsync(ConsoleModule.class,
				RemoteConsoleClientUtils
						.runAsyncCallback(() -> startConsoleActivityAsync(panel,
								consoleActivity)));
	}

	private void startConsoleActivityAsync(AcceptsOneWidget panel,
			ConsoleActivity consoleActivity) {
		panel.setWidget(new ConsolePanel(consoleActivity));
	}
}

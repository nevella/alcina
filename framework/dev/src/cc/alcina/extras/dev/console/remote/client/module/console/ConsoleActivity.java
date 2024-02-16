package cc.alcina.extras.dev.console.remote.client.module.console;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsoleActivityMapper.ConsolePlace;

public class ConsoleActivity implements Activity {
	public ConsoleActivity(ConsolePlace place) {
	}

	@Override
	public String mayStop() {
		return null;
	}

	@Override
	public void onCancel() {
	}

	@Override
	public void onStop() {
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		ConsoleModule.get().startConsoleActivity(panel, this);
	}
}
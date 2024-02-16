package cc.alcina.extras.dev.console.remote.client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;

import cc.alcina.extras.dev.console.remote.client.common.widget.main.RootComponent;
import cc.alcina.extras.dev.console.remote.client.common.widget.nav.NavComponent;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.Client;

public class RemoteConsoleLayout {
	private static RemoteConsoleLayout instance;

	public static RemoteConsoleLayout get() {
		if (instance == null) {
			instance = new RemoteConsoleLayout();
		}
		return instance;
	}

	private FlowPanel head = new FlowPanel();

	private FlowPanel main = new FlowPanel();

	private NavComponent navComponent;

	public final Topic<RemoteConsoleLayoutMessage> topicLayoutMessage = Topic
			.create();

	public NavComponent getNavComponent() {
		return this.navComponent;
	}

	public void init() {
		RootPanel.get().clear();
		RootPanel.get().add(head);
		RootPanel.get().add(main);
		navComponent = new NavComponent();
		head.add(navComponent);
		main.add(new RootComponent());
		initOnceRendered();
	}

	private void initOnceRendered() {
		Client.get().setupPlaceMapping();
		Client.get().initAppHistory();
	}

	public enum RemoteConsoleLayoutMessage {
		FOCUS_COMMAND_BAR
	}
}

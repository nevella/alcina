package cc.alcina.extras.dev.console.remote.client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;

import cc.alcina.extras.dev.console.remote.client.common.widget.main.RootComponent;
import cc.alcina.extras.dev.console.remote.client.common.widget.nav.NavComponent;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.lux.IClientFactory;

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

	public void fire(RemoteConsoleLayoutMessage message) {
		fire(message, null);
	}

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

	public void subscribe(TopicListener<Void> listener,
			RemoteConsoleLayoutMessage message, boolean subscribe) {
		subscribe0(listener, message, subscribe);
	}

	private void fire(RemoteConsoleLayoutMessage message, Object payload) {
		GlobalTopicPublisher.get().publishTopic(topicKey(message), payload);
	}

	private void initOnceRendered() {
		IClientFactory.get().setupPlaceMapping();
		IClientFactory.get().initAppHistory();
	}

	private <T> void subscribe0(TopicListener<T> listener,
			RemoteConsoleLayoutMessage message, boolean subscribe) {
		GlobalTopicPublisher.get().listenerDelta(topicKey(message), listener,
				subscribe);
	}

	private String topicKey(RemoteConsoleLayoutMessage message) {
		return RemoteConsoleLayout.class + "." + message.toString();
	}

	public enum RemoteConsoleLayoutMessage {
		FOCUS_COMMAND_BAR
	}
}

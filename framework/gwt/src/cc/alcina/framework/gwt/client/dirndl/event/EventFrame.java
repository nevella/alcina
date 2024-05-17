package cc.alcina.framework.gwt.client.dirndl.event;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.context.ContextFrame;
import cc.alcina.framework.common.client.context.ContextProvider;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.TopLevelMissedEvent;

public class EventFrame implements ContextFrame {
	public static ContextProvider<Void, EventFrame> contextProvider;

	boolean mobile = GWT.isClient() && BrowserMod.isMobile();

	Topic<TopLevelMissedEvent> topicTopLevelMissedEvent = Topic.create();

	static EventFrame get() {
		return contextProvider.contextFrame();
	}
}

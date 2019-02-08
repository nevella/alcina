package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.web.bindery.event.shared.EventBus;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface IClientFactory {
	public static IClientFactory get() {
		return Registry.impl(IClientFactory.class);
	}

	EventBus getEventBus();

	PlaceHistoryHandler getHistoryHandler();

	default void initAppHistory() {
		getHistoryHandler().handleCurrentHistory();
	}

	void setupPlaceMapping();
}

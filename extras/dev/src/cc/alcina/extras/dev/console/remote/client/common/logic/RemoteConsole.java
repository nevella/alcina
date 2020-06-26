package cc.alcina.extras.dev.console.remote.client.common.logic;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;

import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsoleActivityMapper.ConsolePlace;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.lux.IClientFactory;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@RegistryLocation(registryPoint = IClientFactory.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class RemoteConsole implements IClientFactory {
	public static RemoteConsole get() {
		return (RemoteConsole) IClientFactory.get();
	}

	public static RemoteConsoleModels models() {
		return get().models;
	}

	private RegistryHistoryMapper historyMapper;

	private PlaceHistoryHandler historyHandler;

	private EventBus eventBus = new SimpleEventBus();

	private RemoteConsoleModels models = new RemoteConsoleModels();

	private final PlaceController placeController = new PlaceController(
			eventBus) {
		@Override
		public void goTo(Place newPlace) {
			super.goTo(newPlace);
		}
	};

	@Override
	public EventBus getEventBus() {
		return eventBus;
	}

	@Override
	public PlaceHistoryHandler getHistoryHandler() {
		return this.historyHandler;
	}

	@Override
	public void setupPlaceMapping() {
		historyMapper = Registry.impl(RegistryHistoryMapper.class);
		historyHandler = new PlaceHistoryHandler(historyMapper);
		historyHandler.register(placeController, eventBus,
				() -> new ConsolePlace());
	}
}

package cc.alcina.extras.dev.console.remote.client.common.logic;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;

import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsoleActivityMapper.ConsolePlace;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@ClientInstantiable
public class RemoteConsoleClientImpl extends Client {
	public static RemoteConsoleModels models() {
		return ((RemoteConsoleClientImpl) Client.get()).models;
	}

	private RemoteConsoleModels models = new RemoteConsoleModels();

	@Override
	public void setupPlaceMapping() {
		historyHandler = new PlaceHistoryHandler(RegistryHistoryMapper.get());
		historyHandler.register(placeController, eventBus,
				() -> new ConsolePlace());
	}

	@Override
	protected void createPlaceController() {
		placeController = new PlaceController(eventBus);
	}
}

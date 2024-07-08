package cc.alcina.framework.servlet.dom;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;

import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

public class ClientRemoteImpl extends Client {
	@Override
	protected void createPlaceController() {
		placeController = new PlaceController(eventBus);
	}

	@Override
	public void setupPlaceMapping() {
		historyHandler = new PlaceHistoryHandler(RegistryHistoryMapper.get());
		historyHandler.register(placeController, eventBus, () -> Place.NOWHERE);
	}
}

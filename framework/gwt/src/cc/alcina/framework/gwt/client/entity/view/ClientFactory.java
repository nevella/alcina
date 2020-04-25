package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@RegistryLocation(registryPoint = ClientFactory.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public abstract class ClientFactory {
    static ClientFactory singleton = Registry
            .checkSingleton(ClientFactory.class);

    public static Place currentPlace() {
        return get().getPlaceController().getWhere();
    }

    public static void flushAndRefresh() {
        Runnable runnable = () -> {
            refreshCurrentPlace();
        };
        CommitToStorageTransformListener.flushAndRun(runnable);
    }

    public static ClientFactory get() {
        return Registry.impl(ClientFactory.class);
    }

    public static void goTo(Place newPlace) {
        Runnable runnable = () -> get().placeController.goTo(newPlace);
        CommitToStorageTransformListener.flushAndRun(runnable);
    }

    public static void refreshCurrentPlace() {
        BasePlace place = (BasePlace) currentPlace();
        place.setRefreshed(true);
        place = place.copy();
        goTo(place);
    }

    protected final EventBus eventBus = new SimpleEventBus();

    protected PlaceController placeController;

    protected UiController uiController;

    protected RegistryHistoryMapper historyMapper;

    protected PlaceHistoryHandler historyHandler;

    protected WindowTitleManager windowTitleManager;

    public ClientFactory() {
        createPlaceController();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public RegistryHistoryMapper getHistoryMapper() {
        return this.historyMapper;
    }

    public PlaceController getPlaceController() {
        return this.placeController;
    }

    public UiController getUiController() {
        return this.uiController;
    }

    public void initAppHistory() {
        historyHandler.handleCurrentHistory();
    }

    public boolean isDeveloper() {
        return EntityClientUtils.isTestServer()
                || PermissionsManager.get().isDeveloper();
    }

    public void setupPlaceMapping() {
        historyMapper = Registry.impl(RegistryHistoryMapper.class);
        historyHandler = new PlaceHistoryHandler(historyMapper);
        uiController = new UiController();
    }

    protected abstract void createPlaceController();
}

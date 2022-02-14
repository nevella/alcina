package cc.alcina.framework.gwt.client;

import java.util.Objects;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsRegistryDelegateCreator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.ClientReflectorFactory;
import cc.alcina.framework.common.client.reflection.ModuleReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.SearchRemoteServiceAsync;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.entity.view.EntityClientUtils;
import cc.alcina.framework.gwt.client.entity.view.UiController;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlace.PlaceNavigator;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;


@ClientInstantiable
@Registration.Singleton
public abstract class Client {
	public static CommonRemoteServiceAsync commonRemoteService() {
		return Registry.impl(CommonRemoteServiceAsync.class);
	}

	public static <P extends Place> P currentPlace() {
		return (P) get().getPlaceController().getWhere();
	}

	public static void flushAndRefresh() {
		Runnable runnable = () -> {
			refreshCurrentPlace();
		};
		CommitToStorageTransformListener.flushAndRun(runnable);
	}

	public static Client get() {
		return Registry.impl(Client.class);
	}

	public static void goTo(Place newPlace) {
		Runnable runnable = () -> get().placeController.goTo(newPlace);
		CommitToStorageTransformListener.flushAndRun(runnable);
	}

	public static boolean isCurrentPlace(Place place) {
		return Objects.equals(place, get().placeController.getWhere());
	}

	public static boolean isDeveloper() {
		return EntityClientUtils.isTestServer()
				|| PermissionsManager.get().isDeveloper();
	}

	public static void refreshCurrentPlace() {
		BasePlace place = (BasePlace) currentPlace();
		place.setRefreshed(true);
		place = place.copy();
		goTo(place);
	}

	public static SearchRemoteServiceAsync searchRemoteService() {
		return Registry.impl(SearchRemoteServiceAsync.class);
	}

	protected final EventBus eventBus = new SimpleEventBus();

	protected PlaceController placeController;

	protected UiController uiController;

	protected PlaceHistoryHandler historyHandler;

	public Client() {
		createPlaceController();
	}

	public EventBus getEventBus() {
		return eventBus;
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

	public void setupPlaceMapping() {
		historyHandler = new PlaceHistoryHandler(
				Registry.impl(RegistryHistoryMapper.class));
		uiController = new UiController();
	}

	protected abstract void createPlaceController();

	public static class Init {
		public static long startTime;

		private static boolean complete;

		public static boolean isComplete() {
			return Init.complete;
		}

		public static void preRegistry() {
			startTime = System.currentTimeMillis();
			LiSet liSet = new LiSet();
			CommonUtils.setSupplier = () -> new LightSet();
			LocalDom.mutations.setDisabled(true);
			if (GWT.isScript()) {
				Registry.Internals
						.setDelegateCreator(new JsRegistryDelegateCreator());
			}
			JavascriptKeyableLookup.initJs();
		}

		public static void registry() {
			// initialise clientreflections
			ModuleReflector moduleReflector = ClientReflectorFactory.create();
			moduleReflector.register();
			Reflections.init();
			if (GWT.isScript()) {
				throw new UnsupportedOperationException("TODO - registry");
			}
			// complete=true;
		}
	}

	@ClientInstantiable
	
	@Registration(PlaceNavigator.class)
	public static class PlaceNavigatorImpl implements PlaceNavigator {
		@Override
		public void go(Place place) {
			Client.goTo(place);
		}
	}
}

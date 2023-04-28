package cc.alcina.framework.gwt.client;

import java.util.Objects;
import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsRegistryDelegateCreator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObserver.AppDebug;
import cc.alcina.framework.common.client.reflection.ClientReflectorFactory;
import cc.alcina.framework.common.client.reflection.ModuleReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.SearchRemoteServiceAsync;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.event.VariableDispatchEventBus;
import cc.alcina.framework.gwt.client.entity.view.EntityClientUtils;
import cc.alcina.framework.gwt.client.entity.view.UiController;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlace.PlaceNavigator;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@Reflected
@Registration.Singleton
public abstract class Client {
	public static CommonRemoteServiceAsync commonRemoteService() {
		return Registry.impl(CommonRemoteServiceAsync.class);
	}

	public static <P extends Place> P currentPlace() {
		return (P) get().getPlaceController().getWhere();
	}

	public static VariableDispatchEventBus eventBus() {
		return get().eventBus;
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

	// prefer place.go
	public static void goTo(Place place) {
		Runnable runnable = () -> get().placeController.goTo(place);
		CommitToStorageTransformListener.flushAndRun(runnable);
	}

	public static boolean isCurrentPlace(Place place) {
		return Objects.equals(place, get().placeController.getWhere());
	}

	public static boolean isDeveloper() {
		return EntityClientUtils.isTestServer()
				|| PermissionsManager.isDeveloper();
	}

	public static void refreshCurrentPlace() {
		BasePlace place = (BasePlace) currentPlace();
		place.setRefreshed(true);
		place = place.copy();
		place.go();
	}

	public static void refreshOrGoTo(Place place) {
		if (isCurrentPlace(place)) {
			refreshCurrentPlace();
		} else {
			goTo(place);
		}
	}

	public static SearchRemoteServiceAsync searchRemoteService() {
		return Registry.impl(SearchRemoteServiceAsync.class);
	}

	protected final VariableDispatchEventBus eventBus = new VariableDispatchEventBus();

	protected PlaceController placeController;

	protected UiController uiController;

	protected PlaceHistoryHandler historyHandler;

	public Client() {
		createPlaceController();
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

		public static void init() {
			preRegistry();
			registry();
			/*
			 * initialise localdom, mutations
			 */
			Document.get();
			/*
			 * Attach non-vcs process debugging
			 */
			Optional<AppDebug> appDebug = Registry
					.optional(ProcessObserver.AppDebug.class);
			appDebug.ifPresent(AppDebug::attach);
		}

		public static boolean isComplete() {
			return Init.complete;
		}

		private static void preRegistry() {
			startTime = System.currentTimeMillis();
			CommonUtils.setSupplier = () -> new LightSet();
			if (GWT.isScript()) {
				Registry.Internals
						.setDelegateCreator(new JsRegistryDelegateCreator());
			}
			JavascriptKeyableLookup.initJs();
			Reflections.init();
		}

		private static void registry() {
			ModuleReflector moduleReflector = ClientReflectorFactory.create();
			moduleReflector.register();
			Init.complete = true;
		}
	}

	@Reflected
	@Registration(PlaceNavigator.class)
	public static class PlaceNavigatorImpl implements PlaceNavigator {
		@Override
		public void go(Place place) {
			goTo(place);
		}
	}

	/*
	 * Marker to indicate basic required support classes
	 */
	public static class SupportReachability {
	}
}

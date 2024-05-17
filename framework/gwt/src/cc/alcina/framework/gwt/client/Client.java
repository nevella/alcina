package cc.alcina.framework.gwt.client;

import java.util.Objects;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Document.PerDocumentSupplierGwtImpl;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.context.ContextFrame;
import cc.alcina.framework.common.client.context.ContextProvider;
import cc.alcina.framework.common.client.dom.DomDocument.PerDocumentSupplier;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JsRegistryDelegateCreator;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver.AppDebug;
import cc.alcina.framework.common.client.reflection.ClientReflectorFactory;
import cc.alcina.framework.common.client.reflection.ModuleReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.SearchRemoteServiceAsync;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Url;
import cc.alcina.framework.gwt.client.dirndl.event.EventFrame;
import cc.alcina.framework.gwt.client.dirndl.event.VariableDispatchEventBus;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.entity.view.EntityClientUtils;
import cc.alcina.framework.gwt.client.entity.view.UiController;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlace.PlaceNavigator;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@Reflected
@Registration(Client.class)
public abstract class Client implements ContextFrame {
	public static ContextProvider<Object, Client> contextProvider;

	/**
	 * Utility method for a common pattern (ui bind :: do xxx on place change);
	 */
	public static void addPlaceChangeBinding(Model model, Runnable runnable) {
		model.bindings().addRegistration(() -> Client.eventBus()
				.addHandler(PlaceChangeEvent.TYPE, evt -> runnable.run()));
	}

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
		return contextProvider.contextFrame();
	}

	// prefer place.go (which calls through to this, but is more fluent)
	public static void goTo(Place place) {
		Runnable runnable = () -> get().placeController.goTo(place);
		CommitToStorageTransformListener.flushAndRun(runnable);
	}

	public static boolean has() {
		return contextProvider != null;
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

	protected abstract void createPlaceController();

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

	public static class Init {
		public static long startTime;

		private static boolean complete;

		/*
		 * Called by client-side Client apps only (server-side, init is mostly
		 * once-per-jvm)
		 */
		public static void init() {
			preRegistry();
			registry();
			/*
			 * initialise localdom, mutations
			 */
			Document.initialiseContextProvider(RemoteType.JSO);
			/*
			 * Called in the Impl event loop if in a script context
			 */
			if (!GWT.isScript()) {
				LocalDom.initalize();
			}
			Document.get();
			Registry.register().singleton(PerDocumentSupplier.class,
					new PerDocumentSupplierGwtImpl());
			/*
			 * Attach non-vcs process debugging
			 */
			if (!GWT.isScript()) {
				AppDebug.register();
			}
			/*
			 * Setup client context provider. One client for client apps,
			 * multiple (one per environment) for server
			 */
			contextProvider = ContextProvider.createProvider(
					ctx -> Registry.impl(Client.class), null, null,
					Client.class, false);
			History.contextProvider = ContextProvider.createProvider(
					ctx -> new History(), History::init, null, History.class,
					false);
			Window.Location.contextProvider = ContextProvider.createProvider(
					ctx -> new Window.Location(), null, null,
					Window.Location.class, false);
			Window.Navigator.contextProvider = ContextProvider.createProvider(
					ctx -> new Window.Navigator(), null, null,
					Window.Navigator.class, false);
			Window.Resources.contextProvider = ContextProvider.createProvider(
					ctx -> new Window.Resources(), null, null,
					Window.Resources.class, false);
			EventFrame.contextProvider = ContextProvider.createProvider(
					ctx -> new EventFrame(), null, null, EventFrame.class,
					false);
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

	public Place parsePlace(String strUrl) {
		Url url = Url.parse(strUrl);
		return RegistryHistoryMapper.get().getPlaceIfParseable(url.hash)
				.orElse(null);
	}
}

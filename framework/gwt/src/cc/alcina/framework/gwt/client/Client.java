package cc.alcina.framework.gwt.client;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Document.PerDocumentSupplierGwtImpl;
import com.google.gwt.dom.client.Document.RemoteType;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.behavior.BehaviorRegistry;
import com.google.gwt.place.shared.Place;
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
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentRegistration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.process.ContextObservers;
import cc.alcina.framework.common.client.process.GlobalObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObserver.AppDebug;
import cc.alcina.framework.common.client.reflection.ClientReflectorFactory;
import cc.alcina.framework.common.client.reflection.ModuleReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.SearchRemoteServiceAsync;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Al.Context;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Url;
import cc.alcina.framework.gwt.client.dirndl.event.EventFrame;
import cc.alcina.framework.gwt.client.dirndl.event.VariableDispatchEventBus;
import cc.alcina.framework.gwt.client.dirndl.event.VariableDispatchEventBus.QueuedEvent;
import cc.alcina.framework.gwt.client.entity.view.EntityClientUtils;
import cc.alcina.framework.gwt.client.entity.view.UiController;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlace.PlaceNavigator;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import cc.alcina.framework.servlet.component.romcom.Feature_Romcom_Impl;

@Reflected
@Registration(Client.class)
public abstract class Client implements ContextFrame {
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
			BehaviorRegistry.get().init(true);
			Document.get().onDocumentEventSystemInit();
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
				Al.context = Context.gwt_script;
			} else {
				switch (GWT.getVersion()) {
				case "headless":
					Preconditions.checkState(Al.context != Context.not_set);
					break;
				default:
					Al.context = Context.gwt_dev;
				}
			}
			JavascriptKeyableLookup.initJs();
			Reflections.init();
			/*
			 * Single-threaded app init
			 */
			ContextObservers.registerBaseObservers();
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

	public static ContextProvider<Object, Client> contextProvider;

	private Place pendingPlace;

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

	// Use by preference place.go (which calls through to this, but is more
	// fluent)
	public static void goTo(Place place) {
		get().goTo0(place);
	}

	void goTo0(Place place) {
		pendingPlace = place;
		Runnable runnable = () -> {
			placeController.goTo(place);
			if (pendingPlace == place) {
				pendingPlace = null;
			}
		};
		Runnable runnable2 = () -> eventBus().queued().lambda(runnable)
				.dispatch();
		CommitToStorageTransformListener.flushAndRun(runnable2);
	}

	public static boolean has() {
		return contextProvider != null && contextProvider.hasFrame();
	}

	public static boolean isCurrentPlace(Place place) {
		return Objects.equals(place, get().placeController.getWhere());
	}

	public static boolean isDeveloper() {
		return EntityClientUtils.isTestServer() || Permissions.isDeveloper();
	}

	public static void refreshCurrentPlace() {
		BasePlace place = (BasePlace) currentPlace();
		/*
		 * it turns out overriding equality (with refreshed) in the place is the
		 * best way to handle refresh logic, since it's often modelled in
		 * children as a property - in which case publishing a change event will
		 * not be enough to cause change
		 */
		place.refreshed = true;
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

	protected ActivityManager activityManager;

	public Client() {
		createPlaceController();
	}

	public ActivityManager getActivityManager() {
		return activityManager;
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

	public Place parsePlace(String strUrl) {
		Url url = Url.parse(strUrl);
		return RegistryHistoryMapper.get().getPlaceIfParseable(url.hash)
				.orElse(null);
	}

	public void setupActivityManager() {
		// FIXME - can be a noop, but all dirndl clients should configure +
		// attach their activitymanager here
	}

	protected abstract void createPlaceController();

	/**
	 * Sugar for the most common EventQueue use
	 * 
	 * @return
	 */
	public static QueuedEvent lambda(Runnable runnable) {
		return eventBus().queued().lambda(runnable);
	}

	public static <P extends Place> P pendingPlace() {
		return (P) get().pendingPlace;
	}

	/**
	 * <p>
	 * An important abstraction optimising render queueing:
	 * 
	 * <p>
	 * Only execute the runnable once there's a no-cost way of accessing
	 * currently generated element state. So only execute the runnable once
	 * local dom changes have been flushed to the remote dom - and in the case
	 * of Romcom, once the UI offsets of those dom changes have been returned
	 * from the browser client
	 */
	public static class RenderState {
		/**
		 * 
		 * @param runnable
		 *            A distinct runnable (equal runnables will only be queued
		 *            once per dispatch cycle)
		 */
		public static void queueWithRenderedState(Runnable runnable) {
			/**
			 * wip - ui2 - there *may* be a prettier environment registration
			 * strategy (rather than isBrowser)
			 */
			if (Al.isBrowser()) {
				LocalDom.onFlush(runnable);
			} else {
				Registry.impl(RomcomImpl.class).enqueue(runnable);
			}
		}

		@EnvironmentRegistration
		@Feature.Ref(Feature_Romcom_Impl._WindowState.class)
		public interface RomcomImpl {
			void enqueue(Runnable runnable);
		}

		/**
		 * Debug points for RenderState ssequencing
		 */
		public static class Observable implements GlobalObservable.Debug {
			public enum EventType {
				node_attach, queued_runnable, runnable_mutation_assigned;
			}

			Node gwtNode;

			EventType type;

			Object eventSource;

			Observable(Node gwtNode) {
				this.type = EventType.node_attach;
				this.gwtNode = gwtNode;
			}

			Observable(Object eventSource, EventType type) {
				this.eventSource = eventSource;
				this.type = type;
			}

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder().separator(" - ");
				Integer attachId = gwtNode == null ? null
						: gwtNode.getAttachId();
				format.appendKeyValues("type", type, "attachId", attachId,
						"node",
						gwtNode == null ? null : gwtNode.toNameAttachId(),
						"eventSource", eventSource);
				return format.toString();
			}

			public static void observeNode(Node gwtNode) {
				new Observable(gwtNode).publish();
			}

			public static void eventOcurred(Object eventSource,
					EventType type) {
				new Observable(eventSource, type).publish();
			}
		}

		/**
		 * To debug sequencing, set EnvironmentManager.debugRenderState=true in
		 * the app configuration (which causes an instance of this to be bound
		 * on EnvironmentManager startup)
		 */
		public static class Observer implements ProcessObserver<Observable> {
			static Logger logger = LoggerFactory.getLogger(Observer.class);

			@Override
			public void topicPublished(Observable message) {
				logger.info("[Renderstate] {} :: {}", Ax.appMillis(), message);
			}
		}
	}
}

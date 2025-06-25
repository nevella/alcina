package cc.alcina.framework.servlet.environment;

import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.service.DispatchRefProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivityManager;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModule;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.EnvironmentInitComplete.EnvironmentSettings;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ProcessingException;

/**
 * <p>
 * This is the top-level ROMCOM application class - analagous to a browser
 * Client class, but a level up (to provide hooks for css injection, init,
 * app-specific settings etc)
 * <p>
 * Access - todo - all dom methods must go via env.access().dispatchOnUiThread()
 * <p>
 * FIXME - romcom - move some non-ui to component
 *
 */
public interface RemoteUi {
	/**
	 * <p>
	 * The created client will have its associated activity/place system classes
	 * initialised after this call
	 * 
	 * @return the created client
	 */
	Client createClient();

	void init();

	default void injectCss(String relativePath) {
		StyleInjector.injectNow(Io.read().relativeTo(getClass())
				.resource(relativePath).asString());
	}

	void render();

	default void customizeEnvironmentSettings(EnvironmentSettings settings) {
	}

	default RemoteResolver resolver() {
		return new RemoteResolver();
	}

	public static class TypedPlaceClient<P extends Place>
			extends ClientRemoteImpl {
		Class<P> permittedPlaceSupertype;

		Class<? extends P> defaultPlaceType;

		@Override
		protected void createPlaceController() {
			placeController = new PlaceController(eventBus);
		}

		@Override
		public void setupPlaceMapping() {
			RegistryHistoryMapper mapper = new RegistryHistoryMapper(
					permittedPlaceSupertype);
			Registry.register().singleton(RegistryHistoryMapper.class, mapper);
			historyHandler = new PlaceHistoryHandler(mapper);
			historyHandler.register(placeController, eventBus,
					() -> Reflections.newInstance(defaultPlaceType));
		}

		@Override
		public void setupActivityManager() {
			ActivityMapper activityMapper = new DirectedActivityManager.DefaultMapper();
			activityManager = new DirectedActivityManager(activityMapper,
					Client.eventBus());
		}

		public TypedPlaceClient(Class<P> permittedPlaceSupertype,
				Class<? extends P> defaultPlaceType) {
			this.permittedPlaceSupertype = permittedPlaceSupertype;
			this.defaultPlaceType = defaultPlaceType;
		}

		public TypedPlaceClient(Class<P> defaultPlaceType) {
			this(defaultPlaceType, defaultPlaceType);
		}
	}

	/**
	 * Makes the initial rendering environment available for subsequent
	 * event/binding dispatch
	 */
	public static class RemoteResolver extends ContextResolver {
		public RemoteResolver() {
			super();
			Environment env = Environment.get();
			Consumer<Runnable> uiDispatch = env.access()::invoke;
			dispatch = Ref.of(uiDispatch);
		}
	}

	/**
	 * <p>
	 * Only one active instance permitted per (server) jvm.
	 *
	 * <p>
	 * Example usage is when component the UI of the 'server' jvm (e.g. an
	 * android app)
	 *
	 * 
	 *
	 */
	public interface SingleInstance {
	}

	default void initialiseSettings(String settings) {
	}

	default void addLifecycleHandlers() {
		Window.addPageHideHandler(
				evt -> Environment.get().access().end("pagehide"));
	}

	void end();

	static RemoteUi get() {
		return Environment.get().access().getUi();
	}

	default void flush() {
		Environment.get().access().flush();
	}

	default String getUid() {
		return Environment.get().access().getUid();
	}

	default String getSessionPath() {
		return Environment.get().access().getSessionPath();
	}

	@Property.Not
	void setEnvironment(Environment environment);

	/**
	 * Executed before each event cycle in the pump (analagous to a js event
	 * pump cycle). Use this to say set up a Transaction context
	 */
	default void onEnterIteration() {
	}

	/**
	 * Executed at the end of each event cycle in the pump
	 */
	default void onExitIteration() {
	}

	// Use this to block initial rendering until server-side state is ready
	default void onBeforeEnterContext() {
	}

	boolean isNotifyException(ProcessingException message);

	void reloadApp(String message);

	// complete setup (custom registry etc)
	default void onEnterContext() {
		StatusModule.get();
	}

	default boolean isSendFullExceptionMessage() {
		return true;
	}

	/**
	 * Invoke utils for say posting off-thread notifications to the UI thread
	 */
	public static class Invoke {
		public static void notify(String template, Object... args) {
			invoke(() -> NotificationObservable.of(template, args).publish());
		}

		public static void invoke(Runnable runnable) {
		}

		public static Consumer<Exception> exceptionNotifier() {
			return new Notifier<Exception>(
					CommonUtils::toSimpleExceptionMessage);
		}

		static class Notifier<T> implements Consumer<T> {
			Function<T, String> stringProvider;

			Consumer<Runnable> dispatch;

			Notifier(Function<T, String> stringProvider) {
				this.stringProvider = stringProvider;
				this.dispatch = DispatchRefProvider.get().getDispatch();
			}

			@Override
			public void accept(T t) {
				String message = stringProvider.apply(t);
				dispatch.accept(
						() -> NotificationObservable.of(message).publish());
			}
		}
	}

	default void onExitContext() {
	}
}

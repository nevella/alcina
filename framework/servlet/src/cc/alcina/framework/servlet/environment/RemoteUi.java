package cc.alcina.framework.servlet.environment;

import java.util.function.Consumer;

import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

/**
 * <p>
 * This is the top-level ROMCOM application class - analagous to a browser
 * Client class, but a level up (to provide hooks for css injection, init,
 * app-specific settings etc)
 * <p>
 * Access - todo - all dom methods must go via env.access().dispatchOnUiThread()
 * <p>
 * FIXME - romcom - move some non-ui to component
 */
public interface RemoteUi {
	Client createClient();

	void init();

	default void injectCss(String relativePath) {
		StyleInjector.injectNow(Io.read().relativeTo(getClass())
				.resource(relativePath).asString());
	}

	void render();

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

	default void onEnterIteration() {
	}

	default void onExitIteration() {
	}

	// Use this to block initial rendering until server-side state is ready
	default void onBeforeEnterContext() {
	}
}
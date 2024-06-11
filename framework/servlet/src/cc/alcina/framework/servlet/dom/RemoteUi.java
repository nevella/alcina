package cc.alcina.framework.servlet.dom;

import java.util.function.Consumer;

import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;

/**
 * <p>
 * This is the top-level ROMCOM application class - analagous to a browser
 * Client class, but a level up (to provide hooks for css injection, init,
 * app-specific settings etc)
 */
public interface RemoteUi {
	default Client createClient() {
		return Registry.impl(ClientRemoteImpl.class);
	}

	void init();

	default void injectCss(String relativePath) {
		StyleInjector.injectNow(Io.read().relativeTo(getClass())
				.resource(relativePath).asString());
	}

	void render();

	default RemoteResolver resolver() {
		return new RemoteResolver();
	}

	public static abstract class Abstract implements RemoteUi {
		DirectedLayout layout;

		Environment environment;

		public Environment getEnvironment() {
			return environment;
		}

		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public String toString() {
			return Ax.format("%s::%s", NestedName.get(this),
					Environment.get().connectedClientUid);
		}

		@Override
		public final void render() {
			layout = render0();
		}

		protected abstract DirectedLayout render0();

		@Override
		public void end() {
			if (layout != null) {
				try {
					layout.remove();
					layout = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
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
			Consumer<Runnable> uiDispatch = env::dispatch;
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
		Window.addPageHideHandler(evt -> Environment.get().end("pagehide"));
	}

	default void onBeforeEnterFrame() {
	}

	default void onExitFrame() {
	}

	void end();

	/*
	 * In most cases Environment.get() will also give access to the environment
	 * - but in cases where (say) an off-thread event is being processed, this
	 * access to the environment must be used
	 */
	Environment getEnvironment();

	void setEnvironment(Environment environment);
}

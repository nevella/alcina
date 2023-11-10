package cc.alcina.framework.servlet.dom;

import java.util.function.Consumer;

import com.google.gwt.dom.client.StyleInjector;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;

public interface RemoteUi {
	default Client createClient() {
		return Registry.impl(ClientRemoteImpl.class);
	}

	default RemoteResolver resolver() {
		return new RemoteResolver();
	}

	void init();

	default void injectCss(String relativePath) {
		StyleInjector.injectNow(Io.read().relativeTo(getClass())
				.resource(relativePath).asString());
	}

	void render();

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
}

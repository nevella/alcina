package cc.alcina.framework.servlet.dom;

import java.util.function.Consumer;

import com.google.gwt.dom.client.StyleInjector;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;

public interface RemoteUi {
	default Client createClient() {
		return Registry.impl(ClientRemoteImpl.class);
	}

	Environment getEnvironment();

	void init();

	default void injectCss(String relativePath) {
		StyleInjector.injectNow(Io.read().relativeTo(getClass())
				.resource(relativePath).asString());
	}

	void render();

	default RemoteResolver resolver() {
		return new RemoteResolver();
	}

	void setEnvironment(Environment environment);

	public static abstract class Abstract implements RemoteUi {
		public Environment environment;

		public Environment getEnvironment() {
			return environment;
		}

		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public String toString() {
			return Ax.format("%s::%s", NestedName.get(this),
					environment.connectedClientUid);
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
}

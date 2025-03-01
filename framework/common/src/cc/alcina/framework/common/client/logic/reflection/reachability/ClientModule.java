package cc.alcina.framework.common.client.logic.reflection.reachability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.reflection.ClientReflections;
import cc.alcina.framework.common.client.reflection.ModuleReflector;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.gwt.client.util.Async;

public abstract class ClientModule<T extends ClientModule> {
	protected static Map<Class<? extends ClientModule>, ClientModule> registered = new LinkedHashMap<>();

	protected static <T extends ClientModule> void asyncConstructor(
			Class<T> clazz, Supplier<T> supplier, Consumer<T> consumer) {
	}

	protected ClientModule() {
		Class<? extends ClientModule> moduleClass = getClass();
		register(moduleClass, this);
	}

	protected abstract ModuleReflector createClientReflector();

	private void register(Class<? extends ClientModule> moduleClass,
			ClientModule<T> clientModule) {
		if (!registered.containsKey(moduleClass) && Al.isBrowser()) {
			registered.put(moduleClass, clientModule);
			ModuleReflector moduleReflector = moduleClass == ClientModule.class
					? GWT.create(LeftoverReflector.class)
					: createClientReflector();
			ClientReflections.register(createClientReflector());
			// only register leftover module after first submodule
			// registration
			register(ClientModule.class, null);
		}
	}

	@ReflectionModule(ReflectionModule.LEFTOVER)
	public abstract static class LeftoverReflector extends ModuleReflector {
	}
}
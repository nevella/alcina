package cc.alcina.framework.servlet.dom;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton
public class RemoteDom {
	public static RemoteDom get() {
		return Registry.impl(RemoteDom.class);
	}

	private ConcurrentMap<String, Environment> environments = new ConcurrentHashMap<>();

	public Environment register(RemoteUi ui) {
		Environment environment = new Environment(ui);
		environments.put(environment.uid, environment);
		return environment;
	}
}

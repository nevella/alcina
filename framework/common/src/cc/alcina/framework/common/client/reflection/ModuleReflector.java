package cc.alcina.framework.common.client.reflection;

import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;

public abstract class ModuleReflector {
	public ModuleReflector() {
	}

	public void register() {
		ClientReflections.register(this);
	}

	protected abstract Class forName(String className);

	protected ClassReflector getClassReflector(Class clazz) {
		return getClassReflector_(clazz.getName());
	}

	protected abstract ClassReflector getClassReflector_(String className);

	protected abstract void registerRegistrations();

	@ReflectionModule(value = ReflectionModule.INITIAL, initial = true)
	public static abstract class Initial extends ModuleReflector {
	}
}

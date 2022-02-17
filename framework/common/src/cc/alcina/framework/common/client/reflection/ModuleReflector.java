package cc.alcina.framework.common.client.reflection;

import java.util.Map;
import java.util.function.Supplier;

import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;

//populates ForName, Reflections
@ReflectionModule(value = ReflectionModule.INITIAL, initial = true)
public abstract class ModuleReflector {
	public ModuleReflector() {
	}

	public void register() {
		ClientReflections.register(this);
	}

	protected abstract void registerForNames(Map<String, Class> map);

	protected abstract void registerReflectorSuppliers(
			Map<Class, Supplier<ClassReflector>> map);

	protected abstract void registerRegistrations();
}

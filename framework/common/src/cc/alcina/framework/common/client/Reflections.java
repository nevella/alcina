package cc.alcina.framework.common.client;

import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestart;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
@RegistryLocation(registryPoint=ClearOnAppRestart.class)
public class Reflections {
	@ClearOnAppRestart
	private static Reflections theInstance;

	private static Reflections get() {
		if (theInstance == null) {
			theInstance = new Reflections();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	private PropertyAccessor propertyAccessor;

	public static void registerPropertyAccessor(PropertyAccessor accessor) {
		get().propertyAccessor = accessor;
	}

	public static PropertyAccessor propertyAccessor() {
		return get().propertyAccessor;
	}

	private ObjectLookup objectLookup;

	public static void registerObjectLookup(ObjectLookup ol) {
		get().objectLookup = ol;
	}

	public static ObjectLookup objectLookup() {
		return get().objectLookup;
	}

	private ClassLookup classLookup;

	public static void registerClassLookup(ClassLookup cl) {
		get().classLookup = cl;
	}

	public static ClassLookup classLookup() {
		return get().classLookup;
	}
}

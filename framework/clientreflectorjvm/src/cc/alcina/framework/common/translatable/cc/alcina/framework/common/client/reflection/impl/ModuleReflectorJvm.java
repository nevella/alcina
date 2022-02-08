package cc.alcina.framework.common.client.reflection.impl;

import java.util.Map;
import java.util.function.Supplier;

import com.google.gwt.core.client.GwtScriptOnly;

import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.ModuleReflector;

@GwtScriptOnly
public class ModuleReflectorJvm extends ModuleReflector {

	public ModuleReflectorJvm() {
	}

	public void init() {
	}

	@Override
	protected void registerForNames(Map<String, Supplier<Class>> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void registerReflectorSuppliers(
			Map<String, Supplier<ClassReflector>> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void registerRegistrations() {
		throw new UnsupportedOperationException();
	}
}

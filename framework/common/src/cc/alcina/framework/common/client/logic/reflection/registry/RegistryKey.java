package cc.alcina.framework.common.client.logic.reflection.registry;

import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;

public class RegistryKey {
	private transient Class clazz;

	private String name;

	private transient String simpleName;

	public RegistryKey() {
	}

	public RegistryKey(Class clazz) {
		this.clazz = clazz;
		this.name = clazz.getName();
	}

	public RegistryKey(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object anObject) {
		if (anObject instanceof RegistryKey) {
			return this.name.equals(((RegistryKey) anObject).name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	Class clazz(ClassLookup classLookup) {
		if (clazz == null) {
			clazz = classLookup.getClassForName(name);
		}
		return clazz;
	}

	public String name() {
		return name;
	}

	public String simpleName() {
		if (simpleName == null) {
			simpleName = name.replaceFirst(".+\\.", "");
		}
		return simpleName;
	}

	public void ensureClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public String toString() {
		return name + " (rk)";
	}
}

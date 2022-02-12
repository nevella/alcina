package cc.alcina.framework.common.client.logic.reflection.registry;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.reflection.Reflections;

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

	public String name() {
		return name;
	}

	public String simpleName() {
		if (simpleName == null) {
			Preconditions.checkState(!name.contains(","));
			simpleName = name.replaceFirst(".+\\.", "");
		}
		return simpleName;
	}

	@Override
	public String toString() {
		return name + " (rk)";
	}

	Class clazz() {
		if (clazz == null) {
			clazz = Reflections.forName(name);
		}
		return clazz;
	}
}

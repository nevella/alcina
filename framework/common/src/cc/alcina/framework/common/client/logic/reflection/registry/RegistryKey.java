package cc.alcina.framework.common.client.logic.reflection.registry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.Reflections;

public class RegistryKey {
	public static String nameFor(Class<?>[] classes) {
		if (classes.length == 1) {
			return classes[0].getName();
		}
		if (classes.length == 2) {
			return classes[0].getName() + "," + classes[1].getName();
		}
		return Arrays.stream(classes).map(Class::getName)
				.collect(Collectors.joining(","));
	}

	private transient Class<?>[] classes;

	private String name;

	private transient String simpleName;

	public RegistryKey() {
	}

	public RegistryKey(Class<?>... classes) {
		this.classes = classes;
		this.name = nameFor(classes);
	}

	public RegistryKey(String name) {
		this.name = name;
	}

	public RegistryKey ensureClases(Class<?>... classes) {
		this.classes = classes;
		return this;
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

	Class asSingleClassKey() {
		Preconditions.checkState(classes.length == 1);
		return classes[0];
	}

	Class<?>[] classes() {
		// FIXME - reflection - cleanup
		if (classes == null) {
			try {
				List<Class> classes = Arrays.stream(name.split(","))
						.map(Reflections::forName).collect(Collectors.toList());
				this.classes = (Class[]) classes
						.toArray(new Class[classes.size()]);
			} catch (Exception e) {
				// null will be filtered down-stream - FIXME mvcc.jobs.2 -
				// caching issue
				//
				// days/weeks later...throw if filtered, implies probable
				// reachability issue
				if (!GWT.isScript()) {
					throw new WrappedRuntimeException(e);
				} else {
					e.printStackTrace();
				}
			}
		}
		return classes;
	}
}

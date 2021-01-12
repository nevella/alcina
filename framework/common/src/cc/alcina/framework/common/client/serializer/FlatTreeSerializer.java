package cc.alcina.framework.common.client.serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

/**
 * <p>
 * This class serializes directed graphs as a flat k/v list. Annotations on the
 * serialized classes provide naming information to compress (and make more
 * human-friendly) the key names. The notional algorithm is:
 * </p>
 * <ul>
 * <li>Generate the non-compressed list, producing a list of jsonptr/value pairs
 * <li>Remove where identical to default
 * <li>Compress using class/field annotations:
 * <ul>
 * <li>Remove path segment if default
 * <li>Replace path segment with short name
 * <li>Replace index with (compressed) class name/ordinal tuple if member of
 * polymporhpic collection
 * </ul>
 * </ul>
 * <p>
 * Polymporhpic collections must define the complete list of allowable members,
 * to ensure serialization uniqueness
 * </p>
 * 
 * @author nick@alcina.cc
 *
 */
public class FlatTreeSerializer {
	// FIXME - synchronized
	private static Map<Class, State> defaultValues = new LinkedHashMap<>();

	private static Map<Class, List<Property>> serializationProperties = new LinkedHashMap<>();

	public static <T> T deserialize(Class<T> clazz, String value) {
		if (value == null) {
			return null;
		}
		State state = new State();
		state.clazz = clazz;
		state.keyValues = StringMap.fromPropertyString(value);
		new FlatTreeSerializer(state).deserialize();
		return (T) state.value;
	}

	public static String serialize(Object object) {
		return serialize(object, true);
	}

	public static String serialize(Object object, boolean withRootMetadata) {
		if (object == null) {
			return null;
		}
		State state = new State();
		state.clazz = object.getClass();
		state.withRootMetadata = withRootMetadata;
		state.paths.push(new Path(null, object));
		new FlatTreeSerializer(state).serialize();
		return state.keyValues.toPropertyString();
	}

	State state = new State();

	private FlatTreeSerializer(State state) {
		this.state = state;
	}

	private void deserialize() {
	}

	private State getDefault(Class forClass) {
		return defaultValues.computeIfAbsent(forClass, clazz -> {
			State state = new State();
			state.clazz = clazz;
			state.withRootMetadata = true;
			state.paths.push(new Path(null, Reflections.newInstance(clazz)));
			new FlatTreeSerializer(state).serializeState();
			return state;
		});
	}

	private List<Property> getProperties(Class forClass) {
		return serializationProperties.computeIfAbsent(forClass, clazz -> {
			BeanDescriptor descriptor = Reflections.beanDescriptorProvider()
					.getDescriptor(clazz);
			Property[] propertyArray = descriptor.getProperties();
			return Arrays.stream(propertyArray)
					.sorted(Comparator.comparing(Property::getName))
					.filter(property -> {
						if (property.getMutatorMethod() == null) {
							return false;
						}
						if (property.getAccessorMethod() == null) {
							return false;
						}
						String name = property.getName();
						if (Reflections.propertyAccessor()
								.getAnnotationForProperty(clazz,
										AlcinaTransient.class, name) != null) {
							return false;
						}
						return true;
					}).collect(Collectors.toList());
		});
	}

	private Object getValue(Property property, Object value) {
		try {
			return property.getAccessorMethod().invoke(value, new Object[0]);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void serialize() {
		serializeState();
		getDefault(state.clazz);
	}

	private void serializeState() {
		while (!state.paths.isEmpty()) {
			Path path = state.paths.pop();
			state.visitedPaths.add(path);
			Object value = path.value;
			if (value == null) {
				return;
			}
			Class<? extends Object> clazz = value.getClass();
			if (CommonUtils.stdAndPrimitives.contains(clazz)) {
				return;
			}
			if (clazz.isEnum() || CommonUtils.isEnumSubclass(clazz)) {
				return;
			}
			Path existingPath = state.visitedObjects.put(value, path);
			if (existingPath != null) {
				throw new IllegalStateException(Ax.format(
						"Object %s - multiple references: \n\t%s\n\t%s ", value,
						existingPath, path));
			}
			if (value instanceof Collection) {
				Counter counter = new Counter();
				((Collection) value).forEach(childValue -> {
					Path childPath = new Path(path, childValue);
					childPath.index = counter.getAndIncrement();
					state.paths.push(childPath);
				});
			} else if (value instanceof TreeSerializable) {
				getProperties(clazz).forEach(property -> {
					Object childValue = getValue(property, value);
					Path childPath = new Path(path, childValue);
					childPath.name = property.getName();
					state.paths.push(childPath);
				});
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	private static class Counter {
		int value;

		int getAndIncrement() {
			return value++;
		}
	}

	static class Path {
		public String name;

		Path parent;

		int index;

		Object value;

		Path(Path parent, Object value) {
			this.parent = parent;
			this.value = value;
		}
	}

	static class State {
		boolean withRootMetadata;

		String className;

		Class clazz;

		StringMap keyValues = new StringMap();

		IdentityHashMap<Object, Path> visitedObjects = new IdentityHashMap();

		List<Path> visitedPaths = new ArrayList<>();

		Object value;

		Stack<Path> paths = new Stack<>();
	}
}

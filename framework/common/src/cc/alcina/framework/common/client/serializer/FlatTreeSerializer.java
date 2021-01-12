package cc.alcina.framework.common.client.serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.StringPair;

/**
 * <p>
 * This class serializes directed graphs as a flat k/v list. Annotations on the
 * serialized classes provide naming information to compress (and make more
 * human-friendly) the key names. The notional algorithm is:
 * </p>
 * <ul>
 * <li>Generate the non-compressed list, producing a list of path (parallel with
 * variations to jsonptr)/value pairs
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
		state.pending.add(new VisitedPath(null, object));
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
			state.pending
					.add(new VisitedPath(null, Reflections.newInstance(clazz)));
			new FlatTreeSerializer(state).serializeState();
			return state;
		});
	}

	private List<Property> getProperties(Object value) {
		return serializationProperties.computeIfAbsent(value.getClass(), v -> {
			BeanDescriptor descriptor = Reflections.beanDescriptorProvider()
					.getDescriptor(value);
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
								.getAnnotationForProperty(v.getClass(),
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
		State defaultState = getDefault(state.clazz);
		state.populateMap(defaultState);
	}

	private void serializeState() {
		while (!state.pending.isEmpty()) {
			/*
			 * FIFO
			 */
			VisitedPath path = state.pending.remove(0);
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
			Path existingPath = state.visitedObjects.put(value, path.path);
			if (existingPath != null) {
				throw new IllegalStateException(Ax.format(
						"Object %s - multiple references: \n\t%s\n\t%s ", value,
						existingPath, path));
			}
			if (value instanceof Collection) {
				Counter counter = new Counter();
				((Collection) value).forEach(childValue -> {
					VisitedPath childPath = new VisitedPath(path, childValue);
					childPath.path.index = counter.getAndIncrement();
					state.pending.add(childPath);
				});
			} else if (value instanceof TreeSerializable) {
				getProperties(value).forEach(property -> {
					Object childValue = getValue(property, value);
					VisitedPath childPath = new VisitedPath(path, childValue);
					childPath.path.property = property;
					state.pending.add(childPath);
				});
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	public static abstract class StateCombiner<A, B, C> {
		protected A lastValue;

		protected B intermediateState;

		public List<C> out = new ArrayList<>();

		public void add(A value) {
			if (lastValue == null || !canCombine(value)) {
				flush();
				newIntermediateState();
				lastValue = value;
			}
			addToIntermediate(value);
		}

		public void flush() {
			if (intermediateState != null) {
				C toOut = intermediateToOut();
				if (toOut != null) {
					out.add(toOut);
				}
			}
		}

		protected abstract void addToIntermediate(A value);

		protected abstract boolean canCombine(A value);

		protected abstract C intermediateToOut();

		protected abstract void newIntermediateState();
	}

	private static class Counter {
		int value;

		int getAndIncrement() {
			return value++;
		}
	}

	static class Path {
		public Property property;

		Path parent;

		int index;

		private transient String path;

		public Class type;

		Path(Path parent) {
			this.parent = parent;
		}

		@Override
		public synchronized String toString() {
			if (path == null) {
				String prefix = parent == null ? "" : parent.toString() + ".";
				String suffix = null;
				if (property == null) {
					suffix = String.valueOf(index);
				} else {
					suffix = property.getName();
				}
				path = prefix + suffix;
			}
			return path;
		}
	}

	static class State {
		boolean withRootMetadata;

		String className;

		Class clazz;

		StringMap keyValues = new StringMap();

		IdentityHashMap<Object, Path> visitedObjects = new IdentityHashMap();

		List<VisitedPath> visitedPaths = new ArrayList<>();

		Object value;

		List<VisitedPath> pending = new LinkedList<>();

		public void populateMap(State defaultState) {
			StateCombiner<VisitedPath, List<String>, StringPair> combiner = new StateCombiner<VisitedPath, List<String>, StringPair>() {
				@Override
				protected void addToIntermediate(VisitedPath value) {
					intermediateState
							.add(String.valueOf(value.value.toString()));
				}

				@Override
				protected boolean canCombine(VisitedPath value) {
					return Objects.equals(lastValue.combinerStringPath(),
							value.combinerStringPath());
				}

				@Override
				protected StringPair intermediateToOut() {
					if (intermediateState.isEmpty()) {
						return null;
					}
					return new StringPair(lastValue.combinerStringPath(),
							intermediateState.stream()
									.collect(Collectors.joining(";")));
				}

				@Override
				protected void newIntermediateState() {
					intermediateState = new ArrayList<>();
				}
			};
			if (withRootMetadata) {
				keyValues.put("class",
						visitedPaths.get(0).value.getClass().getName());
				// FIXME - mvcc.flat - application.hash
				keyValues.put("time",
						String.valueOf(System.currentTimeMillis()));
			}
			visitedPaths.stream().skip(1).forEach(combiner::add);
			combiner.flush();
			combiner.out.forEach(p -> keyValues.put(p.s1, p.s2));
		}
	}

	static class VisitedPath {
		Path path;

		Object value;

		VisitedPath parent;

		public VisitedPath(VisitedPath parent, Object value) {
			this.parent = parent;
			this.path = new Path(parent == null ? null : parent.path);
			this.path.type = value == null ? null : value.getClass();
			this.value = value;
		}

		public String combinerStringPath() {
			return path.toString();
		}

		@Override
		public String toString() {
			return Ax.format("%s=%s", path, value);
		}
	}
}

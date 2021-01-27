package cc.alcina.framework.common.client.serializer.flat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.IdentityHashMap;
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
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;

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
 * <p>
 * Constraints: Collections cannot contain nulls, and all-default value
 * TreeSerializable elements will not be serialized
 * </p>
 * 
 * <h2>TODO:</h2>
 * <ul>
 * <li>Improve path() with 'tree-serializable' segments
 * <li>Calculate default elision to nearest TreeSerializable
 * <li>Implement annotations (...tohere)
 * <li>Build resolution map at earch path node (single-prop top-level doesn't
 * elide default btw) - e.g. ServerTask.value
 * <li>Use for ser/deser
 * <li>deser
 * <li>annotation builders (bindableSDef)
 * <li>annotation builders (publication)
 * <li>transformmanager serialization
 * <li>pluggable client sdef ser/deser
 * <li>(later) GWT RPC
 * <li>Apps
 * </ul>
 * 
 * @author nick@alcina.cc
 *
 */
public class FlatTreeSerializer {
	private static String NULL_MARKER = "__fts_NULL__";

	private static Map<Class, State> defaultValues = Registry
			.impl(ConcurrentMapCreator.class).createMap();

	private static Map<Class, List<Property>> serializationProperties = Registry
			.impl(ConcurrentMapCreator.class).createMap();

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
		return state.keyValues.sorted().toPropertyString();
	}

	private static State getDefaultState(Class forClass) {
		return defaultValues.computeIfAbsent(forClass, clazz -> {
			State state = new State();
			state.clazz = clazz;
			state.withRootMetadata = true;
			state.pending
					.add(new VisitedPath(null, Reflections.newInstance(clazz)));
			new FlatTreeSerializer(state).serializeState();
			state.populateMap(false);
			return state;
		});
	}

	private static boolean isLeafValue(Object value) {
		if (value == null) {
			return true;
		}
		Class<? extends Object> clazz = value.getClass();
		if (CommonUtils.stdAndPrimitives.contains(clazz)) {
			return true;
		}
		if (clazz.isEnum() || CommonUtils.isEnumSubclass(clazz)) {
			return true;
		}
		return false;
	}

	State state = new State();

	private FlatTreeSerializer(State state) {
		this.state = state;
	}

	private void deserialize() {
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
		state.populateMap(true);
	}

	private void serializeState() {
		while (!state.pending.isEmpty()) {
			/*
			 * FIFO
			 */
			VisitedPath visitedPath = state.pending.remove(0);
			state.visitedPaths.add(visitedPath);
			Object value = visitedPath.value;
			if (isLeafValue(value)) {
				continue;
			}
			Path existingPath = state.visitedObjects.put(value,
					visitedPath.path);
			if (existingPath != null) {
				throw new IllegalStateException(Ax.format(
						"Object %s - multiple references: \n\t%s\n\t%s ", value,
						existingPath, visitedPath));
			}
			if (value instanceof Collection) {
				Counter counter = new Counter();
				((Collection) value).forEach(childValue -> {
					VisitedPath childPath = new VisitedPath(visitedPath,
							childValue);
					childPath.path.index = counter.getAndIncrement(childValue);
					state.pending.add(childPath);
				});
			} else if (value instanceof TreeSerializable) {
				getProperties(value).forEach(property -> {
					Object childValue = getValue(property, value);
					VisitedPath childPath = new VisitedPath(visitedPath,
							childValue);
					childPath.path.property = property;
					state.pending.add(childPath);
				});
			} else {
				throw new UnsupportedOperationException(
						Ax.format("Invalid value type: %s at %s", value,
								visitedPath.path));
			}
		}
	}

	/*
	 * A generalised 'state combiner' - like a Collector in the Streams package.
	 */
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
		Multimap<Class, List<Object>> indicies = new Multimap<Class, List<Object>>();

		boolean leafValue = false;

		String getAndIncrement(Object childValue) {
			leafValue = isLeafValue(childValue);
			Class clazz = childValue == null ? void.class
					: childValue.getClass();
			indicies.add(clazz, childValue);
			List<Object> list = indicies.get(clazz);
			String leafIndex = list.size() == 1 ? ""
					: (leafValue ? "" : "_") + (list.size() - 1);
			return leafValue ? leafIndex
					: clazz.getName().replace(".", "_") + leafIndex;
		}
	}

	/*
	 * Represents a path to a point in the object value tree
	 */
	static class Path {
		public Property property;

		Path parent;

		String index = "";

		private transient String path;

		public Class type;

		Path(Path parent) {
			this.parent = parent;
		}

		@Override
		public synchronized String toString() {
			if (parent == null) {
				path = "";
				return "";
			}
			if (path == null) {
				String parentPath = parent.toString();
				String prefix = parentPath.length() == 0 ? "" : parentPath;
				String separator = "";
				String suffix = null;
				if (property == null) {
					suffix = index;
				} else {
					suffix = property.getName();
				}
				if (prefix.length() > 0 && suffix.length() > 0) {
					separator = ".";
				}
				path = prefix + separator + suffix;
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

		Class defaultClass;

		State defaultState;

		public void populateMap(boolean elideDefaults) {
			StateCombiner<VisitedPath, List<String>, CombinerNode> combiner = new Combiner();
			if (withRootMetadata) {
				keyValues.put("#class",
						visitedPaths.get(0).value.getClass().getName());
				// FIXME - mvcc.flat - application.hash
				keyValues.put("#time",
						String.valueOf(System.currentTimeMillis()));
			}
			visitedPaths.stream().
			/*
			 * skip the root path & index
			 */
					skip(1).forEach(combiner::add);
			combiner.flush();
			combiner.out.stream()
					.filter(n -> elideDefaults ? !elideDefaults(n) : true)
					.forEach(n -> keyValues.put(n.visitedPath.path.toString(),
							n.value));
		}

		private boolean elideDefaults(CombinerNode n) {
			VisitedPath nearestParent = n.visitedPath
					.nearestTreeSerializableParent();
			ensureDefaultState(nearestParent.value.getClass());
			String segment = n.visitedPath.relativePathSegment(nearestParent);
			String defaultLeafValue = defaultState.keyValues.get(segment);
			return Objects.equals(n.value, defaultLeafValue);
		}

		private void ensureDefaultState(Class<? extends Object> clazz) {
			if (clazz != defaultClass) {
				defaultClass = clazz;
				defaultState = getDefaultState(defaultClass);
			}
		}

		private class Combiner
				extends StateCombiner<VisitedPath, List<String>, CombinerNode> {
			@Override
			protected void addToIntermediate(VisitedPath value) {
				if (value.hasChildren) {
					return;
				}
				intermediateState.add(value.toStringValue());
			}

			@Override
			protected boolean canCombine(VisitedPath value) {
				return lastValue.canCombineWith(value);
			}

			@Override
			protected CombinerNode intermediateToOut() {
				if (intermediateState.isEmpty()) {
					return null;
				}
				return new CombinerNode(lastValue, intermediateState.stream()
						.collect(Collectors.joining(";")));
			}

			@Override
			protected void newIntermediateState() {
				intermediateState = new ArrayList<>();
			}
		}

		private class CombinerNode {
			VisitedPath visitedPath;

			String value;

			public CombinerNode(VisitedPath visitedPath, String value) {
				this.visitedPath = visitedPath;
				this.value = value;
			}
		}
	}

	/*
	 * A visited point in the current object's serialization tree, and the
	 * value.
	 */
	static class VisitedPath {
		// FIXME - 2022 - centralise escaping (ctrl characters etc)
		public static String escape(String str) {
			return str == null || (str.indexOf("\n") == -1
					&& str.indexOf("\\") == -1 && str.indexOf(";") == -1) ? str
							: str.replace("\\", "\\\\").replace("\n", "\\n")
									.replace(";", "\\;");
		}

		Path path;

		Object value;

		VisitedPath parent;

		private boolean hasChildren;

		public VisitedPath(VisitedPath parent, Object value) {
			this.parent = parent;
			this.path = new Path(parent == null ? null : parent.path);
			this.path.type = value == null ? null : value.getClass();
			this.value = value;
			if (parent != null) {
				parent.hasChildren = true;
			}
		}

		public boolean canCombineWith(VisitedPath previous) {
			return isLeafValue(value) && parent == previous.parent
					&& parent.value instanceof Collection;
		}

		public VisitedPath nearestTreeSerializableParent() {
			VisitedPath cursor = this;
			while (true) {
				if (cursor.value instanceof TreeSerializable) {
					return cursor;
				}
				cursor = cursor.parent;
			}
		}

		public String relativePathSegment(VisitedPath from) {
			int offset = from.path.toString().length();
			String pathString = path.toString();
			if (pathString.length() > offset
					&& pathString.charAt(offset) == '.') {
				offset++;
			}
			return pathString.substring(offset);
		}

		@Override
		public String toString() {
			return Ax.format("%s=%s", path, value);
		}

		public String toStringValue() {
			if (value == null) {
				return NULL_MARKER;
			}
			if (value instanceof Date) {
				return String.valueOf(((Date) value).getTime());
			} else if (value instanceof String) {
				return escape(value.toString());
			} else {
				return value.toString();
			}
		}
	}
}

package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.eclipse.jdt.core.dom.Modifier;

import com.google.gwt.dev.util.collect.IdentityHashMap;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.DomainDescriptor;
import cc.alcina.framework.common.client.domain.MemoryStat;
import cc.alcina.framework.common.client.domain.MemoryStat.Counter;
import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.domain.MemoryStat.ObjectMemory;
import cc.alcina.framework.common.client.domain.ReverseDateProjection;
import cc.alcina.framework.common.client.domain.TrieProjection;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.PermissibleFieldFilter.AllFieldsFilter;
import cern.colt.Timer;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator.MemoryLayoutSpecification;

public abstract class DomainStoreDescriptor extends DomainDescriptor
		implements MemoryStatProvider {
	protected DomainSegmentLoader domainSegmentLoader;

	@Override
	public MemoryStat addMemoryStats(MemoryStat parent) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		perClass.values()
				.forEach(descriptor -> descriptor.addMemoryStats(self));
		return self;
	}

	public DomainSegmentLoader getDomainSegmentLoader() {
		return domainSegmentLoader;
	}

	public abstract Class<? extends DomainTransformRequestPersistent>
			getDomainTransformRequestPersistentClass();

	public Class<? extends ClassRef> getShadowClassRefClass() {
		throw new UnsupportedOperationException();
	}

	public Class<? extends DomainTransformEvent>
			getShadowDomainTransformEventPersistentClass() {
		throw new UnsupportedOperationException();
	}

	public boolean isUseTransformDbCommitSequencing() {
		return true;
	}

	public void saveSegmentData() {
		throw new UnsupportedOperationException();
	}

	/**
	 * TODO: counter stores shallow and deep size of entities (deep: reachable
	 * non-entity objects) pass up layer
	 * 
	 * check stats for cache vs descriptor
	 * 
	 * sort output
	 * 
	 * @author nick@alcina.cc
	 *
	 */
	public static class ObjectMemoryImpl extends ObjectMemory {
		public static final Predicate<Class> entityAndMapAndCollectionFilter = clazz -> {
			return Entity.class.isAssignableFrom(clazz)
					|| Map.class.isAssignableFrom(clazz)
					|| Set.class.isAssignableFrom(clazz);
		};

		GraphProjection projection = new GraphProjection();

		IdentityHashMap seen = new IdentityHashMap<>();

		ShallowObjectSizeCalculator objectSizeCalculator = new ShallowObjectSizeCalculator();

		Set<String> shallowOnly = new TreeSet<>();

		Set<String> notShallow = new TreeSet<>();

		private Map<Class, Boolean> shallowResult = new HashMap<>();

		public ObjectMemoryImpl() {
			projection.setFilters(new AllFieldsFilter() {
				@Override
				public boolean permitTransient(Field field) {
					return Map.class.isAssignableFrom(field.getDeclaringClass())
							|| Collection.class.isAssignableFrom(
									field.getDeclaringClass());
				}
			}, null);
		}

		@Override
		public void dumpStats() {
			Ax.err("shallow\n========");
			Ax.out(shallowOnly);
			Ax.err("\nnot shallow\n========");
			Ax.out(notShallow);
		}

		@Override
		public boolean isMemoryStatProvider(Class<? extends Object> clazz) {
			return MemoryStatProvider.class.isAssignableFrom(clazz);
		}

		@Override
		public void walkStats(Object o, Counter counter,
				Predicate<Object> filter) {
			if (o == null) {
				return;
			}
			Class<? extends Object> clazz = o.getClass();
			/*
			 * guaranteed not primitive
			 */
			if (filter != null && !filter.test(o)) {
				return;
			}
			if (seen.put(o, o) != null) {
				return;
			}
			counter.count++;
			long size = getShallowObjectSize(o);
			counter.size += size;
			counter.perClassSize.merge(clazz, size, (v1, v2) -> v1 + v2);
			counter.perClassCount.merge(clazz, 1L, (v1, v2) -> v1 + v2);
			if (shallowOnly(clazz)) {
				return;
			}
			try {
				if (clazz.isArray()) {
					Class<?> componentType = clazz.getComponentType();
					if (componentType.isPrimitive()) {
						return;
					} else {
						Object[] array = (Object[]) o;
						for (int idx = 0; idx < array.length; idx++) {
							walkStats(array[idx], counter, filter);
						}
					}
				} else {
					List<Field> fields = projection.getFieldsForClass(clazz);
					for (Field field : fields) {
						if (Modifier.isStatic(field.getModifiers())) {
							continue;
						}
						if (field.getType().isPrimitive()) {
							// done, allocated in container
							continue;
						} else {
							Object value = field.get(o);
							walkStats(value, counter, filter);
						}
					}
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		private long getShallowObjectSize(Object o) {
			if (o == null) {
				return 0;
			}
			return objectSizeCalculator.getSize(o);
		}

		private boolean shallowOnly(Class<? extends Object> clazz) {
			return shallowResult.computeIfAbsent(clazz, this::shallowOnly0);
		}

		private boolean shallowOnly0(Class<? extends Object> clazz) {
			if (Map.class.isAssignableFrom(clazz)
					|| Collection.class.isAssignableFrom(clazz)
					|| Date.class.isAssignableFrom(clazz)) {
				return false;
			}
			boolean drop = false;
			if (clazz.getName().matches(
					"(java.(nio|security|io|beans)|javax|com.sun|sun.|javassist.|org.postgresql|org.slf4j|com.github|org.apache|"
							+ "java.util.(GregorianCalendar|Timer)|"
							+ "java.lang.(ref|Thread)|"
							+ "java.util.concurrent.(locks|Thread|atomic)).*")) {
				drop = true;
			}
			if (clazz == Thread.class || clazz == ThreadGroup.class
					|| clazz == Timer.class) {
				drop = true;
			}
			if (drop) {
				shallowOnly.add(clazz.getName());
				return true;
			} else {
				notShallow.add(clazz.getName());
				return false;
			}
		}

		private static class ShallowObjectSizeCalculator {
			private static long getPrimitiveFieldSize(final Class<?> type) {
				if (type == boolean.class || type == byte.class) {
					return 1;
				}
				if (type == char.class || type == short.class) {
					return 2;
				}
				if (type == int.class || type == float.class) {
					return 4;
				}
				if (type == long.class || type == double.class) {
					return 8;
				}
				throw new AssertionError(
						"Encountered unexpected primitive type "
								+ type.getName());
			}

			private ObjectSizeCalculator objectSizeCalculator;

			private Map<Class, Long> instanceSize = new LinkedHashMap<>();

			private Method classInfoMethod;

			private Field objectSizeField;

			private long arrayHeaderSize;

			private int objectPadding;

			private long referenceSize;

			public ShallowObjectSizeCalculator() {
				try {
					MemoryLayoutSpecification memoryLayoutSpecification = ObjectSizeCalculator
							.getEffectiveMemoryLayoutSpecification();
					this.objectSizeCalculator = new ObjectSizeCalculator(
							memoryLayoutSpecification);
					classInfoMethod = Arrays
							.stream(objectSizeCalculator.getClass()
									.getDeclaredMethods())
							.filter(m -> m.getName().equals("getClassSizeInfo"))
							.findFirst().get();
					classInfoMethod.setAccessible(true);
					Object objectSizeInfo = classInfoMethod.invoke(
							objectSizeCalculator,
							new Object[] { Object.class });
					objectSizeField = objectSizeInfo.getClass()
							.getDeclaredField("objectSize");
					objectSizeField.setAccessible(true);
					arrayHeaderSize = memoryLayoutSpecification
							.getArrayHeaderSize();
					objectPadding = memoryLayoutSpecification
							.getObjectPadding();
					referenceSize = memoryLayoutSpecification
							.getReferenceSize();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}

			public long getSize(Object o) {
				Class<? extends Object> clazz = o.getClass();
				if (clazz.isArray()) {
					final Class<?> arrayClass = clazz;
					final Class<?> componentType = arrayClass
							.getComponentType();
					final int length = Array.getLength(o);
					if (componentType.isPrimitive()) {
						return arraySize(length,
								getPrimitiveFieldSize(componentType));
					} else {
						return arraySize(length, referenceSize);
					}
				} else {
					return instanceSize.computeIfAbsent(clazz,
							this::getClassInstanceSize);
				}
			}

			private long arraySize(final int length, final long elementSize) {
				return roundTo(arrayHeaderSize + length * elementSize,
						objectPadding);
			}

			private long getClassInstanceSize(Class clazz) {
				try {
					Object objectSizeInfo = classInfoMethod.invoke(
							objectSizeCalculator, new Object[] { clazz });
					return objectSizeField.getLong(objectSizeInfo);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}

			private long roundTo(final long x, final int multiple) {
				return ((x + multiple - 1) / multiple) * multiple;
			}
		}
	}

	@RegistryLocation(registryPoint = TestSupport.class, implementationType = ImplementationType.SINGLETON)
	public static abstract class TestSupport {
		public static DomainStoreDescriptor.TestSupport get() {
			return Registry.impl(DomainStoreDescriptor.TestSupport.class);
		}

		public abstract <T extends Entity> T createReversedDateEntityInstance();

		public abstract <T extends Entity> T
				createTrieEntityInstance(String key);

		public abstract ReverseDateProjection getReversedDateProjection();

		public abstract TrieProjection getTrieProjection();

		public abstract Class<? extends Entity> getTypeWithLazyProperties();
	}
}

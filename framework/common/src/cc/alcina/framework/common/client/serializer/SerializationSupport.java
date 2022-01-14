package cc.alcina.framework.common.client.serializer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.beans.BeanDescriptor;
import com.totsp.gwittir.client.beans.Property;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Annotations;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient.TransienceContext;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;

class SerializationSupport {
	private Map<Class, List<Property>> serializationProperties = Registry
			.impl(ConcurrentMapCreator.class).create();

	private Map<Class, Map<String, PropertyReflector>> serializationReflectors = Registry
			.impl(ConcurrentMapCreator.class).create();

	private static Map<Class, Class> solePossibleImplementation = Registry
			.impl(ConcurrentMapCreator.class).create();

	private SerializationSupport() {
	}


	static SerializationSupport serializationInstance() {
		SerializationSupport support = new SerializationSupport();
		support.types = AlcinaTransient.Support.getTransienceContexts();
		return support;
	}

	public static final Comparator<Property> PROPERTY_COMPARATOR = new Comparator<Property>() {
		@Override
		public int compare(Property f1, Property f2) {
			if (f1.getAccessorMethod() != null) {
				Class checkType = f1.getAccessorMethod().getDeclaringClass();
				boolean entityType = Reflections.isAssignableFrom(Entity.class,
						checkType);
				if (entityType) {
					/*
					 * serialize id, localid, other - to ensure population
					 * before hash
					 */
					int idx1 = f1.getName().equals("id") ? 0
							: f1.getName().equals("localId") ? 1 : 2;
					int idx2 = f2.getName().equals("id") ? 0
							: f2.getName().equals("localId") ? 1 : 2;
					if (idx1 != idx2) {
						return idx1 - idx2;
					}
				}
			}
			return f1.getName().compareTo(f2.getName());
		}
	};

	public PropertyReflector getPropertyReflector(Class<? extends Object> clazz,
			String propertyName) {
		Map<String, PropertyReflector> map = serializationReflectors
				.computeIfAbsent(clazz, c -> {
					return getProperties0(c).stream()
							.collect(Collectors.toMap(Property::getName,
									p -> Reflections.propertyAccessor()
											.getPropertyReflector(c,
													p.getName())));
				});
		return map.get(propertyName);
	}

	public static Class solePossibleImplementation(Class type) {
		Class clazz = Domain.resolveEntityClass(type);
		return solePossibleImplementation.computeIfAbsent(clazz, valueClass -> {
			if (Reflections.isEffectivelyFinal(clazz)
					|| (Reflections.isAssignableFrom(Entity.class, clazz) &&
			// FXIME - meta - Modifiers.nonAbstract emul
			// non-abstract entity classes have no serializable subclasses (but
			// do have mvcc subclass...)
			Reflections.classLookup().isNonAbstract(clazz))) {
				return valueClass;
			} else if (Registry.get().lookupSingle(PersistentImpl.class, clazz,
					false) != Void.class) {
				return Registry.get().lookupSingle(PersistentImpl.class, clazz,
						false);
			} else {
				return null;
			}
		});
	}

	// Optimisation: share support for all deserializers - they don't use
	// context transience.
	static SerializationSupport deserializationInstance = new SerializationSupport();

	private TransienceContext[] types;

	private List<Property> getProperties0(Class forClass) {
		Class clazz = Domain.resolveEntityClass(forClass);
		return serializationProperties.computeIfAbsent(clazz, valueClass -> {
			BeanDescriptor descriptor = Reflections.beanDescriptorProvider()
					.getDescriptor(Reflections.classLookup()
							.getTemplateInstance(clazz));
			Property[] propertyArray = descriptor.getProperties();
			return Arrays.stream(propertyArray).sorted(PROPERTY_COMPARATOR)
					.filter(property -> {
						if (property.getMutatorMethod() == null) {
							return false;
						}
						if (property.getAccessorMethod() == null) {
							return false;
						}
						String name = property.getName();
						AlcinaTransient alcinaTransient = Annotations.resolve(
								valueClass, name, AlcinaTransient.class);
						if (alcinaTransient != null) {
							if (AlcinaTransient.Support
									.isTransient(alcinaTransient, types)) {
								return false;
							}
						}
						PropertySerialization propertySerialization = getPropertySerialization(
								valueClass, name);
						if (propertySerialization != null
								&& propertySerialization.ignore()) {
							return false;
						}
						return true;
					}).collect(Collectors.toList());
		});
	}

	List<Property> getProperties(Object value) {
		return getProperties0(value.getClass());
	}

	static PropertySerialization getPropertySerialization(Class<?> clazz,
			String propertyName) {
		TypeSerialization typeSerialization = Annotations.resolve(clazz,
				TypeSerialization.class);
		PropertySerialization annotation = null;
		if (typeSerialization != null) {
			for (PropertySerialization p : typeSerialization.properties()) {
				if (p.name().equals(propertyName)) {
					annotation = p;
					break;
				}
			}
		}
		if (annotation == null) {
			annotation = Annotations.resolve(clazz, propertyName,
					PropertySerialization.class);
		}
		return annotation;
	}
}

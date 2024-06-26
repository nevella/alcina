package cc.alcina.framework.common.client.serializer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.PersistentImpl;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient.TransienceContext;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.Annotations;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;

class SerializationSupport {
	private static Map<Class, Class> solePossibleImplementation = Registry
			.impl(ConcurrentMapCreator.class).create();

	// Optimisation: share support for all deserializers - they don't use
	// context transience. Note that on the client this will have context
	// (CLIENT) transience set to avoid unncessary reflection
	static SerializationSupport deserializationInstance = new SerializationSupport();

	static PropertySerialization getPropertySerialization(Property property) {
		Class<?> clazz = property.getOwningType();
		TypeSerialization typeSerialization = Annotations.resolve(clazz,
				TypeSerialization.class);
		PropertySerialization annotation = null;
		if (typeSerialization != null) {
			for (PropertySerialization p : typeSerialization.properties()) {
				if (p.name().equals(property.getName())) {
					annotation = p;
					break;
				}
			}
		}
		if (annotation == null) {
			annotation = Annotations.resolve(clazz, property.getName(),
					PropertySerialization.class);
		}
		if (annotation == null
				&& Reflections.isAssignableFrom(Collection.class,
						property.getType())
				&& property.getTypeBounds().bounds.size() == 1) {
			PropertySerialization.Impl impl = new PropertySerialization.Impl();
			impl.setTypes(
					new Class[] { property.getTypeBounds().bounds.get(0) });
			annotation = impl;
		}
		return annotation;
	}

	static SerializationSupport serializationInstance() {
		SerializationSupport support = new SerializationSupport();
		support.types = AlcinaTransient.Support.getTransienceContexts();
		return support;
	}

	public static Class solePossibleImplementation(Class type) {
		Class<?> clazz = Domain.resolveEntityClass(type);
		return solePossibleImplementation.computeIfAbsent(clazz, valueClass -> {
			boolean effectivelyFinal = Reflections.isEffectivelyFinal(clazz);
			if (!effectivelyFinal) {
				ClassReflector<?> classReflector = Reflections.at(clazz);
				effectivelyFinal = classReflector.isFinal()
						|| (Reflections.isAssignableFrom(Entity.class, clazz) &&
				// FXIME - reflection - Modifiers.nonAbstract emul
				// non-abstract entity classes have no serializable subclasses
				// (but
				// do have mvcc subclass...)
				//
				// so this code says "yes, effectively final if a non-abstract
				// entity subclass
				//
								!classReflector.isAbstract());
			}
			if (effectivelyFinal) {
				return valueClass;
			} else if (PersistentImpl.hasImplementation(clazz)) {
				return PersistentImpl.getImplementation(clazz);
			} else {
				return null;
			}
		});
	}

	private Map<Class, List<Property>> serializationProperties = Registry
			.impl(ConcurrentMapCreator.class).create();

	private Map<Class, Map<String, Property>> serializationPropertiesByName = Registry
			.impl(ConcurrentMapCreator.class).create();

	TransienceContext[] types;

	private SerializationSupport() {
	}

	List<Property> getProperties(Class type) {
		return getProperties0(type);
	}

	private List<Property> getProperties0(Class forClass) {
		Class clazz = ClassUtil.resolveEnumSubclassAndSynthetic(forClass);
		return serializationProperties.computeIfAbsent(clazz, valueClass -> {
			ClassReflector<?> classReflector = Reflections.at(valueClass);
			return classReflector.properties().stream()
					.sorted(new PropertyComparator(classReflector))
					.filter(property -> {
						if (property.isReadOnly()) {
							return false;
						}
						if (property.isWriteOnly()) {
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
								property);
						if (propertySerialization != null
								&& propertySerialization.ignore()) {
							return false;
						}
						return true;
					}).collect(Collectors.toList());
		});
	}

	public Property getProperty(Class<?> clazz, String propertyName) {
		Map<String, Property> map = serializationPropertiesByName
				.computeIfAbsent(clazz, c -> {
					return getProperties0(c).stream()
							.collect(Collectors.toMap(Property::getName,
									p -> (Property) Reflections.at(c)
											.property(p.getName())));
				});
		return map.get(propertyName);
	}

	static final class PropertyComparator implements Comparator<Property> {
		TypeSerialization.PropertyOrder order;

		public PropertyComparator(ClassReflector<?> classReflector) {
			TypeSerialization typeSerialization = classReflector
					.annotation(TypeSerialization.class);
			order = typeSerialization == null ? PropertyOrder.NAME
					: typeSerialization.propertyOrder();
		}

		@Override
		public int compare(Property f1, Property f2) {
			if (f1.isReadable()) {
				boolean entityType = Reflections.isAssignableFrom(Entity.class,
						f1.getOwningType());
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
			return order == PropertyOrder.NAME
					? f1.getName().compareTo(f2.getName())
					:
					// prserve order (original order will be field)
					0;
		}
	}
}

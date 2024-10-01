package cc.alcina.framework.common.client.serializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

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
import cc.alcina.framework.common.client.reflection.TypeBounds;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.TypeSerializer;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.CollectionCreators.ConcurrentMapCreator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;

/*
 * Serialization varies according to the 'context transience' - the transience
 * of properties varies according to the context flags (CLIENT, RPC, JOB) etc
 * 
 * This affects the *computation* of transience (effectively the PropertyNode
 * instances used by the ReflectiveSerializer instance). Since tat computation
 * is identical for a given set of transience flags, it's cached here
 */
class SerializerReflection {
	private static Map<Class, Class> solePossibleImplementation = Registry
			.impl(ConcurrentMapCreator.class).create();

	static SerializerReflection get(boolean defaultCollectionTypes) {
		return get(new TransienceContext[0], defaultCollectionTypes);
	}

	static SerializerReflection get(TransienceContext[] transienceContexts,
			boolean defaultCollectionTypes) {
		TransienceContext[] contextTransienceContexts = AlcinaTransient.Support
				.getTransienceContexts();
		Set<TransienceContext> set = new LinkedHashSet<>();
		for (TransienceContext transienceContext : contextTransienceContexts) {
			set.add(transienceContext);
		}
		for (TransienceContext transienceContext : transienceContexts) {
			set.add(transienceContext);
		}
		SerializationModifiers key = new SerializationModifiers(set,
				defaultCollectionTypes);
		return reflectionByModifiers.computeIfAbsent(key,
				SerializerReflection::new);
	}

	static class SerializationModifiers {
		Set<TransienceContext> transienceContexts;

		boolean defaultCollectionTypes;

		SerializationModifiers(Set<TransienceContext> transienceContexts,
				boolean defaultCollectionTypes) {
			this.transienceContexts = transienceContexts;
			this.defaultCollectionTypes = defaultCollectionTypes;
		}

		@Override
		public int hashCode() {
			return Objects.hash(transienceContexts, defaultCollectionTypes);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SerializationModifiers) {
				SerializationModifiers o = (SerializationModifiers) obj;
				return CommonUtils.equals(transienceContexts,
						o.transienceContexts, defaultCollectionTypes,
						o.defaultCollectionTypes);
			} else {
				return false;
			}
		}
	}

	static Map<SerializationModifiers, SerializerReflection> reflectionByModifiers = Registry
			.impl(ConcurrentMapCreator.class).create();

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

	static SerializerReflection
			serializationInstance(boolean defaultCollectionTypes) {
		return get(defaultCollectionTypes);
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

	boolean defaultCollectionTypes;

	private Map<Class, List<Property>> serializationProperties = Registry
			.impl(ConcurrentMapCreator.class).create();

	private Map<Class, Map<String, Property>> serializationPropertiesByName = Registry
			.impl(ConcurrentMapCreator.class).create();

	private Map<Class, TypeNode> typeNodes = Registry
			.impl(ConcurrentMapCreator.class).create();

	/*
	 * this must be synchronized,
	 */
	TypeNode getTypeNode(Class clazz) {
		if (ClassUtil.isEnumSubclass(clazz)) {
			clazz = clazz.getSuperclass();
		}
		TypeNode typeNode = typeNodes.get(clazz);
		if (typeNode == null) {
			synchronized (typeNodes) {
				/*
				 * double-checking not essential
				 */
				typeNode = new TypeNode(clazz);
				synchronized (typeNode) {
					typeNodes.put(clazz, typeNode);
					typeNode.init();
				}
			}
		} else {
			if (!typeNode.initialised) {
				boolean initialised = false;
				synchronized (typeNode) {
					// the lock will be released on initialisation
					Preconditions.checkState(typeNode.initialised);
				}
			}
		}
		return typeNode;
	}

	/*
	 * Encapsulates serialization info for a type in the current serialization
	 * context. Every GraphNode has a type node - if the graphnode corresponds
	 * to Java null, the type node type will be Void
	 */
	class TypeNode {
		volatile boolean initialised;

		Class<? extends Object> type;

		SerializerReflection.TypeSerializerForType serializerLocation;

		List<PropertyNode> properties = new ArrayList<>();

		Map<Property, PropertyNode> propertyMap = AlcinaCollections
				.newHashMap();

		Map<String, PropertyNode> propertyNameMap = AlcinaCollections
				.newHashMap();

		ClassReflector classReflector;

		TypeSerializer serializer;

		/*
		 * split constructor and init to handle recursive lookup
		 */
		TypeNode(Class type) {
			this.type = type;
		}

		void init() {
			classReflector = Reflections.at(type);
			serializerLocation = ReflectiveSerializer.resolveSerializer(type);
			serializer = serializerLocation.typeSerializer;
			List<Property> list = getProperties(type);
			for (Property property : list) {
				try {
					PropertyNode propertyNode = new PropertyNode(property);
					propertyMap.put(property, propertyNode);
					propertyNameMap.put(property.getName(), propertyNode);
					properties.add(propertyNode);
				} catch (Exception e) {
					Ax.out("Exclude: %s", property.toLocationString());
				}
			}
			initialised = true;
		}

		boolean hasProperties() {
			return properties.size() > 0;
		}

		Object newInstance() {
			return classReflector.newInstance();
		}

		PropertyNode propertyNode(Property property) {
			return propertyMap.get(property);
		}

		PropertyNode propertyNode(String name) {
			return propertyNameMap.get(name);
		}

		Object templateInstance() {
			return classReflector.templateInstance();
		}

		/*
		 * Encapsulates serialization info for a property in the current
		 * serialization context.
		 */
		class PropertyNode {
			TypeNode exactChildTypeNode;

			TypeNode exactTypeNode;

			Property property;

			PropertySerialization propertySerialization;

			PropertyNode(Property property) {
				this.property = property;
				Class type = property.getType();
				Class exactType = SerializerReflection
						.solePossibleImplementation(type);
				if (exactType != null) {
					exactTypeNode = getTypeNode(exactType);
				}
				if (defaultCollectionTypes) {
					// must be synced with the the hardcoded handling in the
					// SerializerOptions.elideTypeInfo()
					// method
					if (type == List.class) {
						exactTypeNode = getTypeNode(ArrayList.class);
					} else if (type == Map.class) {
						exactTypeNode = getTypeNode(LinkedHashMap.class);
					} else if (type == Set.class) {
						exactTypeNode = getTypeNode(LinkedHashSet.class);
					}
				}
				propertySerialization = ReflectiveSerializer
						.getPropertySerialization(property.getOwningType(),
								property.getName());
				if (propertySerialization != null
						&& propertySerialization.types().length == 1) {
					exactChildTypeNode = getTypeNode(
							propertySerialization.types()[0]);
				} else {
					if (Reflections.isAssignableFrom(Collection.class,
							property.getType())) {
						TypeBounds typeBounds = property.getTypeBounds();
						if (typeBounds.bounds.size() == 1) {
							Class<?> elementType = typeBounds.bounds.get(0);
							Class soleImplementationType = SerializerReflection
									.solePossibleImplementation(elementType);
							if (soleImplementationType != null) {
								exactChildTypeNode = getTypeNode(
										soleImplementationType);
							}
						}
					}
				}
			}

			String name() {
				return property.getName();
			}

			@Override
			public String toString() {
				FormatBuilder format = new FormatBuilder();
				format.line(property);
				format.appendIfNotBlankKv("exactTypeNode", exactTypeNode);
				format.appendIfNotBlankKv("exactChildTypeNode",
						exactChildTypeNode);
				return format.toString();
			}

			public TypeNode typeNodeAndVerify(Class<? extends Object> type,
					boolean child) {
				TypeNode typeNode = getTypeNode(type);
				if (child) {
					//
				} else {
					typeNode.serializerLocation.verifyType(property.getType());
				}
				return typeNode;
			}
		}
	}

	TransienceContext[] types;

	private SerializerReflection(SerializationModifiers modifiers) {
		types = modifiers.transienceContexts.toArray(
				new TransienceContext[modifiers.transienceContexts.size()]);
		defaultCollectionTypes = modifiers.defaultCollectionTypes;
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
						if (ReflectiveSerializer.applicationPropertyFilter != null) {
							if (!ReflectiveSerializer.applicationPropertyFilter
									.test(property)) {
								return false;
							}
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

	static class TypeSerializerForType {
		Class actualType;

		ReflectiveSerializer.TypeSerializer typeSerializer;

		public TypeSerializerForType(Class actualType,
				ReflectiveSerializer.TypeSerializer typeSerializer) {
			this.actualType = actualType;
			this.typeSerializer = typeSerializer;
		}

		private Map<Class, Boolean> verifiedDeclaredTypes = Registry
				.impl(ConcurrentMapCreator.class).create();

		public void verifyType(Class declaredType) {
			if (declaredType != null
					&& !typeSerializer.handlesDeclaredTypeSubclasses()) {
				if (actualType != Enum.class && actualType != Entity.class) {
					if (verifiedDeclaredTypes.containsKey(declaredType)) {
						return;
					}
					// i.e. declaredtype is a supertype of serializertype
					if (Reflections.isAssignableFrom(declaredType,
							actualType)) {
						verifiedDeclaredTypes.put(declaredType, true);
					} else {
						throw new IllegalStateException(Ax.format(
								"Declared type %s cannot be serialized by resolved serializer for type %s",
								declaredType, actualType));
					}
				}
			}
		}
	}
}

package cc.alcina.framework.servlet.knowns;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.cache.Domain;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryProvider;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.entityaccess.KnownNodePersistent;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.registry.ClassLoaderAwareRegistryProvider;
import cc.alcina.framework.entity.util.SynchronizedDateFormat;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.knowns.Knowns.ValueType;

public abstract class Knowns {
	public static KnownRoot root;

	public static void register(KnownRoot root) {
		Knowns.root = root;
		root.restore();
		root.persist();
	}

	public static void reconcile(KnownNode node, boolean fromPersistent) {
		try {
			LooseContext.pushWithTrue(
					KryoUtils.CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER);
			RegistryProvider registryProvider = Registry
					.getProvider();
			if (registryProvider instanceof ClassLoaderAwareRegistryProvider) {
				ClassLoaderAwareRegistryProvider clRegistryProvider = (ClassLoaderAwareRegistryProvider) registryProvider;
				LooseContext.set(KryoUtils.CONTEXT_OVERRIDE_CLASSLOADER,
						clRegistryProvider.getServletLayerClassloader());
			}
			if (fromPersistent) {
				fromPersistent(node);
			} else {
				toPersistent(node);
			}
		} finally {
			LooseContext.pop();
		}
	}

	enum ValueType {
		DATA_TYPE, KNOWN_NODE, KNOWN_NODE_SET, KRYO_PERSISTABLE
	}

	private static synchronized void fromPersistent(KnownNode node) {
		Class<? extends KnownNodePersistent> persistentClass = Registry.get()
				.lookupSingle(KnownNodePersistent.class, void.class);
		if (node.parent == null) {
			node.persistent = Domain.byProperty(persistentClass, "parent",
					null);
		}
		Stack<KnownNode> nodes = new Stack<KnownNode>();
		nodes.push(node);
		GraphProjection graphProjection = new GraphProjection();
		try {
			while (!nodes.isEmpty()) {
				node = nodes.pop();
				KnownNodePersistent persistent = node.persistent;
				if (persistent == null) {
					continue;
				}
				persistent = persistent.domain().domainVersion();
				StringMap properties = StringMap
						.fromPropertyString(persistent.getProperties());
				Field[] fields = graphProjection.getFieldsForClass(node);
				for (Field field : fields) {
					Type type = field.getGenericType();
					ValueType valueType = getValueType(type);
					switch (valueType) {
					case DATA_TYPE:
					case KRYO_PERSISTABLE: {
						String value = properties.get(field.getName());
						field.set(node,
								fromStringValue(value, field, valueType));
						break;
					}
					case KNOWN_NODE: {
						Optional<KnownNodePersistent> persistentChild = persistent
								.getChildren().stream().filter(n -> n.getName()
										.equals(field.getName()))
								.findFirst();
						if (persistentChild.isPresent()) {
							Object object = field.get(node);
							KnownNode child = (KnownNode) object;
							child.persistent = persistentChild.get();
							nodes.push(child);
						}
						break;
					}
					default:
						throw new UnsupportedOperationException();
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static ValueType getValueType(Type type) {
		Class classType = null;
		if (type instanceof Class) {
			classType = (Class) type;
		} else {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			classType = (Class) parameterizedType.getRawType();
		}
		if (GraphProjection.isPrimitiveOrDataClass(classType)) {
			return ValueType.DATA_TYPE;
		}
		if (KnownNode.class.isAssignableFrom(classType)) {
			return ValueType.KNOWN_NODE;
		}
		if (Set.class.isAssignableFrom(classType)) {
			return ValueType.KNOWN_NODE_SET;
		}
		return ValueType.KRYO_PERSISTABLE;
	}

	private static synchronized void toPersistent(KnownNode node) {
		Stack<KnownNode> nodes = new Stack<KnownNode>();
		nodes.push(node);
		GraphProjection graphProjection = new GraphProjection();
		Class<? extends KnownNodePersistent> persistentClass = Registry.get()
				.lookupSingle(KnownNodePersistent.class, void.class);
		CachingMap<KnownNodePersistent, KnownNodePersistent> writeable = new CachingMap<>(
				kn -> kn.writeable());
		List<KnownNode> replaceWithPersistent = new ArrayList<>();
		try {
			while (!nodes.isEmpty()) {
				node = nodes.pop();
				KnownNodePersistent persistent = node.persistent;
				if (persistent == null) {
					persistent = Domain.create(persistentClass);
					persistent.setName(node.name);
					if (node.parent != null) {
						persistent.setParent(node.parent.persistent);
						writeable.get(node.parent.persistent).domain()
								.addToProperty(persistent, "children");
					}
					node.persistent = persistent;
					replaceWithPersistent.add(node);
				} else {
					persistent = writeable.get(persistent);
				}
				StringMap properties = new StringMap();
				Field[] fields = graphProjection.getFieldsForClass(node);
				for (Field field : fields) {
					if (Modifier.isTransient(field.getModifiers())) {
						continue;
					}
					Object value = field.get(node);
					Type type = field.getGenericType();
					ValueType valueType = getValueType(type);
					switch (valueType) {
					case DATA_TYPE:
					case KRYO_PERSISTABLE: {
						properties.put(field.getName(),
								toStringValue(value, field, valueType));
						break;
					}
					case KNOWN_NODE: {
						if (value == null) {
							Ax.runtimeException(
									"Field %s.%s is null - must be instantiated",
									node.path(), field.getName());
						}
						nodes.push((KnownNode) value);
						break;
					}
					default:
						throw new UnsupportedOperationException();
					}
				}
				persistent.setProperties(properties.toPropertyString());
			}
			DomainTransformLayerWrapper wrapper = ServletLayerUtils
					.pushTransforms(null, true, true);
			replaceWithPersistent.stream().forEach(n -> {
				HiliLocator hiliLocator = wrapper.locatorMap
						.get(n.persistent.getLocalId());
				n.persistent = Domain.find(hiliLocator);
			});
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static String toStringValue(Object value, Field field,
			ValueType valueType) {
		if (value == null) {
			return null;
		}
		if (valueType == ValueType.KRYO_PERSISTABLE) {
			return KryoUtils.serializeToBase64(value);
		}
		if (value.getClass() == Integer.class
				|| value.getClass() == String.class
				|| value.getClass() == Double.class
				|| value.getClass() == Float.class
				|| value.getClass() == Short.class
				|| value.getClass() == Boolean.class
				|| value.getClass() == Long.class || value instanceof Enum) {
			return value.toString();
		} else if (value.getClass() == Date.class) {
			return dateFormat.format((Date) value);
		}
		throw new RuntimeException("not implemented");
	}

	private static String dateFormatStr = "dd-MMM-yyyy,hh:mm:ss";

	private static SimpleDateFormat dateFormat = new SynchronizedDateFormat(
			dateFormatStr);

	private static Object fromStringValue(String value, Field field,
			ValueType valueType) throws ParseException {
		Class type = field.getType();
		if (value == null) {
			return null;
		}
		if (valueType == ValueType.KRYO_PERSISTABLE) {
			return KryoUtils.deserializeFromBase64(value, type);
		}
		if (type == String.class) {
			return value;
		}
		if (type == Long.class) {
			return Long.valueOf(value);
		}
		if (type == Double.class) {
			return Double.valueOf(value);
		}
		if (type == Integer.class) {
			return Integer.valueOf(value);
		}
		if (type == Boolean.class) {
			return Boolean.valueOf(value);
		}
		if (type == Date.class) {
			return dateFormat.parse(value);
		}
		if (type.isEnum()) {
			return Enum.valueOf(type, value);
		}
		throw new RuntimeException("not implemented");
	}
}

package cc.alcina.framework.servlet.knowns;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.KnownNodeMetadata.KnownNodeProperty;
import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;
import cc.alcina.framework.common.client.csobjects.KnownStatusRule;
import cc.alcina.framework.common.client.csobjects.OpStatus;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryProvider;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.cache.DomainRunner;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.registry.ClassLoaderAwareRegistryProvider;
import cc.alcina.framework.entity.util.SynchronizedDateFormat;

/*
 * FIXME - a lot of public statics here that should be encapsulated (consequence of subclassing persistence)
 */
@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class Knowns {
	public static Object reachableKnownsModificationNotifier = new Object();

	public static KnownRoot root;

	private static String dateFormatStr = "dd-MMM-yyyy,hh:mm:ss";

	private static SimpleDateFormat dateFormat = new SynchronizedDateFormat(
			dateFormatStr);

	public static long lastModified = System.currentTimeMillis();

	public static void reconcile(KnownNode node, boolean fromPersistent) {
		if (Ax.isTest() && !ResourceUtilities.is("testPersistenceEnabled")) {
			return;
		}
		try {
			LooseContext.pushWithTrue(
					KryoUtils.CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER);
			RegistryProvider registryProvider = Registry.getProvider();
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

	public static void register(KnownRoot root) {
		Knowns.root = root;
		root.restore();
		root.persist();
	}

	public static void shutdown() {
		synchronized (reachableKnownsModificationNotifier) {
			reachableKnownsModificationNotifier.notifyAll();
		}
	}

	private static synchronized KnownRenderableNode
			fromPersistent(KnownNode node) {
		return DomainRunner.get(() -> fromPersistent0(node));
	}

	private static synchronized KnownRenderableNode
			fromPersistent0(KnownNode node) {
		return node.persistence.fromPersistent(node);
	}

	public static Object fromStringValue(String path, String value, Field field,
			ValueType valueType) throws ParseException {
		Class type = field.getType();
		return fromStringValue(path,value, field.getName(), type, valueType);
	}

	public static Object fromStringValue(String path,String value, String fieldName,
			Class type, ValueType valueType) throws ParseException {
		if (value == null) {
			return null;
		}
		if (valueType == ValueType.KRYO_PERSISTABLE) {
			try {
				return KryoUtils.deserializeFromBase64(value, type);
			} catch (Exception e) {
				Ax.err("Unable to deserialize %s:%s", path,fieldName);
				try {
					return type.newInstance();
				} catch (Exception e1) {
					throw new WrappedRuntimeException(e1);
				}
			}
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
			try {
				ZonedDateTime zdt = DateTimeFormatter.ISO_DATE_TIME.parse(value,
						ZonedDateTime::from);
				Date epochDate = new Date(zdt.toInstant().toEpochMilli());
				return epochDate;
			} catch (Exception e) {
				try {
					return dateFormat.parse(value);
				} catch (Exception e1) {
					Ax.err("Unable to parse %s:%s", path,fieldName);
					e1.printStackTrace();
					return null;
				}
			}
		}
		if (type.isEnum()) {
			return Enum.valueOf(type, value);
		}
		throw new RuntimeException("not implemented");
	}

	public static ValueType getValueType(Type type) {
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

	private static String debugStatusPath=null;
	public static void handleStatusRule(KnownRenderableNode node) {
		if(Objects.equals(debugStatusPath,node.path())){
			int debug=3;
		}
		if (node.field != null) {
			Field field = (Field) node.field;
			KnownStatusRule rule = field.getAnnotation(KnownStatusRule.class);
			if (rule == null) {
				return;
			}
			Registry.get()
					.lookupImplementation(KnownStatusRuleHandler.class,
							rule.name(), "ruleName", true)
					.handleRule(field, node, rule);
		} else {
			if (node.nodeMetadata != null) {
				KnownStatusRule rule = node.nodeMetadata.statusRule;
				if (rule == null) {
					return;
				}
				Registry.get()
						.lookupImplementation(KnownStatusRuleHandler.class,
								rule.name(), "ruleName", true)
						.handleRule(node.nodeMetadata, node, rule);
			}
		}
	}

	public static void mapToRenderablePropertyNode(KnownRenderableNode parent,
			String value, Object typedValue, String name, Field field,
			KnownNodeProperty propertyMetadata) {
		try {
			KnownRenderableNode propertyNode = new KnownRenderableNode();
			propertyNode.parent = parent;
			parent.children.add(propertyNode);
			propertyNode.value = value;
			propertyNode.name = name;
			propertyNode.property = true;
			propertyNode.typedValue = typedValue;
			propertyNode.field = field;
			propertyNode.propertyMetadata = propertyMetadata;
			if (propertyMetadata != null) {
				Class typeClass = Class
						.forName(propertyNode.propertyMetadata.typeName);
				if (typeClass == Date.class) {
					propertyNode.dateValue = (Date) fromStringValue(parent.path(),value, name,
							typeClass, ValueType.DATA_TYPE);
				} else if (typeClass == OpStatus.class) {
					propertyNode.opStatusValue = (OpStatus) fromStringValue(parent.path(),
							value, name, typeClass, ValueType.DATA_TYPE);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static synchronized void toPersistent(KnownNode node) {
		node.persistence.toPersistent(node);
	}

	public static String toStringValue(Object value, Field field,
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
			ZonedDateTime zdt = ZonedDateTime
					.ofInstant(((Date) value).toInstant(), ZoneId.of("UTC"));
			return DateTimeFormatter.ISO_DATE_TIME.format(zdt);
		}
		throw new RuntimeException("not implemented");
	}

	static KnownRenderableNode renderableRoot() {
		try {
			LooseContext.pushWithTrue(
					KryoUtils.CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER);
			return fromPersistent(Knowns.root);
		} finally {
			LooseContext.pop();
		}
	}

	public enum ValueType {
		DATA_TYPE, KNOWN_NODE, KNOWN_NODE_SET, KRYO_PERSISTABLE
	}
}

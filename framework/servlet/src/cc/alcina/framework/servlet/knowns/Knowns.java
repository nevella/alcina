package cc.alcina.framework.servlet.knowns;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.KnownNodeMetadata.KnownNodeProperty;
import cc.alcina.framework.common.client.csobjects.KnownRenderableNode;
import cc.alcina.framework.common.client.csobjects.KnownStatusRule;
import cc.alcina.framework.common.client.csobjects.OpStatus;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryProvider;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.registry.ClassLoaderAwareRegistryProvider;
import cc.alcina.framework.entity.util.SynchronizedDateFormat;

/**
 * <p>
 * A persistent tree of facts about a system (such as a server cluster)
 * 
 * <p>
 * Each fact is modelled as a {@link KnownNode}
 */
@Registration(ClearStaticFieldsOnAppShutdown.class)
public class Knowns {
	public static Object reachableKnownsModificationNotifier = new Object();

	public static KnownRoot root;

	public static final Locale EN_NZ = new Locale("en", "NZ", "");

	public static final DateFormatSymbols SYMBOLS_EN = DateFormatSymbols
			.getInstance(EN_NZ);

	private static String dateFormatStr = "dd-MMM-yyyy,hh:mm:ss";

	private static SimpleDateFormat dateFormat = new SynchronizedDateFormat(
			dateFormatStr, SYMBOLS_EN);

	public static long lastModified = System.currentTimeMillis();

	private static String debugStatusPath = null;

	private static synchronized KnownRenderableNode
			fromPersistent(KnownNode node) {
		return node.persistence.fromPersistent(node);
	}

	public static Object fromStringValue(String path, String value, Field field,
			ValueType valueType) throws ParseException {
		Class type = field.getType();
		return fromStringValue(path, value, field.getName(), type, valueType);
	}

	public static Object fromStringValue(String path, String value,
			String fieldName, Class type, ValueType valueType)
			throws ParseException {
		if (value == null) {
			return null;
		}
		if (valueType == ValueType.KRYO_PERSISTABLE) {
			try {
				return KryoUtils.deserializeFromBase64(value, type);
			} catch (Exception e) {
				Ax.err("Unable to deserialize %s:%s", path, fieldName);
				try {
					return type.getDeclaredConstructor().newInstance();
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
					Ax.err("Unable to parse %s:%s", path, fieldName);
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

	public static void handleStatusRule(KnownRenderableNode node) {
		if (Objects.equals(debugStatusPath, node.path())) {
			int debug = 4;
		}
		if (node.getField() != null) {
			Field field = (Field) node.getField();
			KnownStatusRule rule = field.getAnnotation(KnownStatusRule.class);
			if (rule == null) {
				return;
			}
			Registry.query(KnownStatusRuleHandler.class).forEnum(rule.name())
					.handleRule(field, node, rule);
			Registry.impl(KnownNodeAppLogic.class).processNodeRule(node, rule);
		} else {
			if (node.getNodeMetadata() != null) {
				KnownStatusRule rule = node.getNodeMetadata().getStatusRule();
				if (rule == null) {
					return;
				}
				Registry.query(KnownStatusRuleHandler.class)
						.forEnum(rule.name())
						.handleRule(node.getNodeMetadata(), node, rule);
				Registry.impl(KnownNodeAppLogic.class).processNodeRule(node,
						rule);
			}
		}
	}

	public static void mapToRenderablePropertyNode(KnownRenderableNode parent,
			String value, Object typedValue, String name, Field field,
			KnownNodeProperty propertyMetadata) {
		try {
			KnownRenderableNode propertyNode = new KnownRenderableNode();
			propertyNode.setParent(parent);
			parent.getChildren().add(propertyNode);
			propertyNode.setValue(value);
			propertyNode.setName(name);
			propertyNode.setProperty(true);
			propertyNode.setTypedValue(typedValue);
			propertyNode.setField(field);
			propertyNode.setPropertyMetadata(propertyMetadata);
			if (propertyMetadata != null) {
				Class typeClass = Class.forName(
						propertyNode.getPropertyMetadata().getTypeName());
				if (typeClass == Date.class) {
					propertyNode.setDateValue(
							(Date) fromStringValue(parent.path(), value, name,
									typeClass, ValueType.DATA_TYPE));
				} else if (typeClass == OpStatus.class) {
					propertyNode.setOpStatusValue(
							(OpStatus) fromStringValue(parent.path(), value,
									name, typeClass, ValueType.DATA_TYPE));
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void reconcile(KnownNode node, boolean fromPersistent) {
		if (Ax.isTest() && !Configuration.is("testPersistenceEnabled")) {
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

	static KnownRenderableNode renderableRoot() {
		try {
			LooseContext.pushWithTrue(
					KryoUtils.CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER);
			return fromPersistent(Knowns.root);
		} finally {
			LooseContext.pop();
		}
	}

	public static void shutdown() {
		synchronized (reachableKnownsModificationNotifier) {
			reachableKnownsModificationNotifier.notifyAll();
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

	/**
	 * For app-level overrides
	 *
	 * 
	 */
	@Registration.Singleton
	public static class KnownNodeAppLogic {
		public void processNodeRule(KnownRenderableNode node,
				KnownStatusRule rule) {
			// No-op
		}
	}

	public enum ValueType {
		DATA_TYPE, KNOWN_NODE, KNOWN_NODE_SET, KRYO_PERSISTABLE
	}
}

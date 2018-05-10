package cc.alcina.framework.entity.entityaccess.knowns;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.cache.Domain;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.domaintransform.DomainTransformLayerWrapper;
import cc.alcina.framework.entity.logic.EntityLayerPersister;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.SynchronizedDateFormat;

public abstract class Knowns {
	public static KnownRoot root;

	public static void register(KnownRoot root) {
		Knowns.root = root;
		root.restore();
		root.persist();
	}

	public static void reconcile(KnownNode node, boolean fromPersistent) {
		if (fromPersistent) {
			fromPersistent(node);
		} else {
			toPersistent(node);
		}
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
					Class<?> type = field.getType();
					boolean valuePersistable = GraphProjection
							.isPrimitiveOrDataClass(type);
					String value = properties.get(field.getName());
					if (valuePersistable) {
						field.set(node, fromStringValue(value, field));
					} else {
						Optional<KnownNodePersistent> persistentChild = persistent
								.getChildren().stream().filter(n -> n.getName()
										.equals(field.getName()))
								.findFirst();
						if (persistentChild.isPresent()) {
							KnownNode child = (KnownNode) field.get(node);
							child.persistent = persistentChild.get();
							nodes.push(child);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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
					Class<?> type = field.getType();
					boolean valuePersistable = GraphProjection
							.isPrimitiveOrDataClass(type);
					Object value = field.get(node);
					if (valuePersistable) {
						properties.put(field.getName(),
								toStringValue(value, field));
					} else {
						if (value == null) {
							Ax.runtimeException(
									"Field %s.%s is null - must be instantiated",
									node.path(), field.getName());
						}
						nodes.push((KnownNode) value);
					}
				}
				persistent.setProperties(properties.toPropertyString());
			}
			DomainTransformLayerWrapper wrapper = Registry.impl(EntityLayerPersister.class).pushTransforms(null, true, true);
			replaceWithPersistent.stream().forEach(n -> {
				HiliLocator hiliLocator = wrapper.locatorMap.get(n.persistent.getLocalId());
				n.persistent = Domain.find(hiliLocator);
			});
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static String toStringValue(Object value, Field field) {
		if (value == null) {
			return null;
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

	private static Object fromStringValue(String value, Field field)
			throws ParseException {
		Class type = field.getType();
		if (value == null) {
			return null;
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

package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CountingMap;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;

public class GraphWalker {
	IdentityHashMap reached = new IdentityHashMap();

	CountingMap<Class> counts = new CountingMap<Class>();

	Map<Class, Field[]> projectableFields = new HashMap<Class, Field[]>();

	private BiConsumer<GraphProjectionContext, Object> preChildrenConsumer;

	private BiConsumer<GraphProjectionContext, Object> postChildrenConsumer;

	public Field[] getFieldsForClass(Object projected) throws Exception {
		Class<? extends Object> clazz = projected.getClass();
		Field[] result = projectableFields.get(clazz);
		if (result == null) {
			List<Field> allFields = new ArrayList<Field>();
			Class c = clazz;
			while (c != Object.class) {
				Field[] fields = c.getDeclaredFields();
				for (Field field : fields) {
					if (Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					if (Modifier.isTransient(field.getModifiers())) {
						continue;
					}
					field.setAccessible(true);
					allFields.add(field);
				}
				c = c.getSuperclass();
			}
			result = (Field[]) allFields.toArray(new Field[allFields.size()]);
			projectableFields.put(clazz, result);
		}
		return result;
	}

	public void walk(Object object,
			BiConsumer<GraphProjectionContext, Object> preChildrenConsumer,
			BiConsumer<GraphProjectionContext, Object> postChildrenConsumer) {
		this.preChildrenConsumer = preChildrenConsumer;
		this.postChildrenConsumer = postChildrenConsumer;
		try {
			walk0(object, null);
			// System.out.println(CommonUtils.join(counts.toLinkedHashMap(true)
			// .entrySet(), "\n"));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void walk0(Object source, GraphProjectionContext context)
			throws Exception {
		if (source == null) {
			return;
		}
		Class c = source.getClass();
		if (GraphProjection.isPrimitiveOrDataClass(c)) {
			counts.add(c);
			return;
		}
		if (reached.containsKey(source)) {
			return;
		}
		if (preChildrenConsumer != null) {
			preChildrenConsumer.accept(context, source);
		}
		reached.put(source, source);
		counts.add(c);
		if (source instanceof Collection) {
			for (Object obj : (Collection) source) {
				walk0(obj, context);
			}
			return;
		} else if (source instanceof Map) {
			Set<Map.Entry> entrySet = ((Map) source).entrySet();
			for (Entry entry : entrySet) {
				walk0(entry.getKey(), context);
				walk0(entry.getValue(), context);
			}
			return;
		}
		Class<? extends Object> sourceClass = source.getClass();
		Field[] fields = getFieldsForClass(source);
		for (Field field : fields) {
			Object value = field.get(source);
			GraphProjectionContext childContext = new GraphProjectionContext();
			childContext.adopt(c, field, context, null, source);
			walk0(value, childContext);
		}
		if (postChildrenConsumer != null) {
			postChildrenConsumer.accept(context, source);
		}
	}
}

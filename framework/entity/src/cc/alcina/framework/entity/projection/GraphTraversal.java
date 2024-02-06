package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Multimap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

public class GraphTraversal {
	public static Multimap<Class, List<?>> getClassStats(Object object,
			Predicate<Class> filter) {
		Multimap<Class, List<?>> map = new Multimap<>();
		new GraphTraversal().withFilter(filter).traverse(object,
				o -> map.add(o.getClass(), o));
		return map;
	}

	private Predicate<Class> filter;

	private Reference2ReferenceOpenHashMap reached = new Reference2ReferenceOpenHashMap();

	private Stack pending = new Stack();

	private GraphProjection projectionHelper = new GraphProjection();

	private void add(Object object) {
		if (object == null) {
			return;
		}
		Class<? extends Object> clazz = object.getClass();
		if (filter != null && !filter.test(clazz)) {
			return;
		}
		if (clazz == Module.class) {
			// not introspectable(much)
			return;
		}
		if (clazz.getName().startsWith("jdk.internal")) {
			// not introspectable(much)
			return;
		}
		if (!reached.containsKey(object)) {
			pending.push(object);
		}
	}

	public void traverse(Object from, Consumer<?> consumer) {
		try {
			traverse0(from, consumer);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void traverse0(Object from, Consumer consumer) throws Exception {
		pending.push(from);
		while (pending.size() > 0) {
			Object object = pending.pop();
			consumer.accept(object);
			reached.put(object, object);
			if (object instanceof Collection) {
				((Collection) object).forEach(this::add);
			} else if (object instanceof Map) {
				((Map<?, ?>) object).entrySet().forEach(e -> {
					add(e.getKey());
					add(e.getValue());
				});
			} else {
				Class clazz = object.getClass();
				if (clazz.getModule().isOpen(clazz.getPackageName())) {
					List<Field> fields = projectionHelper
							.getFieldsForClass(clazz);
					for (Field f : fields) {
						if (GraphProjection.isPrimitiveOrDataClass(f.getType())
								&& !Date.class.isAssignableFrom(f.getType())) {
						} else {
							add(f.get(object));
						}
					}
				}
			}
		}
	}

	public GraphTraversal withFilter(Predicate<Class> filter) {
		this.filter = filter;
		return this;
	}
}

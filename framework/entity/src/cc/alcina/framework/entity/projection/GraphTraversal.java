package cc.alcina.framework.entity.projection;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Multimap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

public class GraphTraversal {
	private Predicate<Class> filter;

	public static Multimap<Class, List<?>> getClassStats(Object object,
			Predicate<Class> filter) {
		Multimap<Class, List<?>> map = new Multimap<>();
		new GraphTraversal().withFilter(filter).traverse(object,
				o -> map.add(o.getClass(), o));
		return map;
	}

	public GraphTraversal withFilter(Predicate<Class> filter) {
		this.filter = filter;
		return this;
	}

	private Reference2ReferenceOpenHashMap reached = new Reference2ReferenceOpenHashMap();

	private Stack pending = new Stack();

	private GraphProjection projectionHelper = new GraphProjection();

	public void traverse(Object from, Consumer<?> consumer) {
		try {
			traverse0(from, consumer);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private void add(Object object) {
		if (object == null) {
			return;
		}
		if (!filter.test(object.getClass())) {
			return;
		}
		if (!reached.containsKey(object)) {
			pending.push(object);
		}
	}

	private void traverse0(Object from, Consumer consumer) throws Exception {
		pending.push(from);
		while (pending.size() > 0) {
			Object object = pending.pop();
			if (object.getClass().getName().contains("MultiDiff")) {
				int debug = 3;
			}
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
				List<Field> fields = projectionHelper
						.getFieldsForClass(object.getClass());
				for (Field f : fields) {
					if (GraphProjection.isPrimitiveOrDataClass(f.getType())) {
					} else {
						add(f.get(object));
					}
				}
			}
		}
	}
}

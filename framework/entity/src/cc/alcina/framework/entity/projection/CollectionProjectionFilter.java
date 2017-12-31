package cc.alcina.framework.entity.projection;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionContext;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDataFilter;

public class CollectionProjectionFilter implements GraphProjectionDataFilter {
	@SuppressWarnings("unchecked")
	public <T> T filterData(T original, T projected,
			GraphProjectionContext context, GraphProjection graphProjection)
			throws Exception {
		if (original.getClass().isArray()) {
			int n = Array.getLength(original);
			for (int i = 0; i < n; i++) {
				Object source = Array.get(original, i);
				Array.set(projected, i,
						graphProjection.project(source, context));
			}
		}
		if (original instanceof Collection) {
			return (T) graphProjection.projectCollection((Collection) original,
					context);
		}
		if (original instanceof Map) {
			return (T) projectMap((Map) original, context, graphProjection);
		}
		return projected;
	}

	@Override
	public <T> boolean projectIntoCollection(T value, T projected,
			GraphProjectionContext context) {
		return true;
	}

	private Object projectMap(Map map, GraphProjectionContext context,
			GraphProjection graphProjection) throws Exception {
		Map m = map.getClass().newInstance();
		Iterator itr = map.keySet().iterator();
		Object value, key;
		for (; itr.hasNext();) {
			key = itr.next();
			value = map.get(key);
			Object pKey = graphProjection.project(key, context);
			if (key == null || pKey != null) {
				m.put(pKey, graphProjection.project(value, context));
			}
		}
		return m;
	}
}
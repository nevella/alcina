package cc.alcina.framework.entity.projection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.util.Multimap;
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

	private Object projectMap(Map map, GraphProjectionContext context,
			GraphProjection graphProjection) throws Exception {
		Map m = null;
		if (map instanceof Multimap) {
			m = new Multimap();
		} else if (map instanceof LinkedHashMap) {
			m = new LinkedHashMap();
		} else {
			m = new HashMap();
		}
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
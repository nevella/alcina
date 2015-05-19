package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache.PdOperator;

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongObjectScatterMap;
import com.carrotsearch.hppc.cursors.LongCursor;

public class PropertyStoreLookup<T, H extends HasIdAndLocalId> extends
		CacheLookup<T, H> {
	private PropertyStore propertyStore;

	private LongObjectScatterMap<LongArrayList> lookup = new LongObjectScatterMap<>();

	private PdOperator pd;

	public PropertyStoreLookup(PropertyStoreLookupDescriptor descriptor,
			PropertyStore store) {
		super(descriptor);
		this.propertyStore = store;
		store.addLookup(this);
	}

	@Override
	public Set<Long> get(T k1) {
		Long id = (Long) k1;
		if (lookup.containsKey(id)) {
			return convertArr(lookup.get(id));
		}
		return null;
	}

	private Set<Long> convertArr(LongArrayList longArrayList) {
		Set<Long> res = new TreeSet<Long>();
		for (LongCursor c : longArrayList) {
			res.add(c.value);
		}
		return res;
	}

	public void initPds() {
		this.pd = propertyStore.getDescriptor(descriptor.propertyPath);
	}

	public void insert(Object[] objects, long id) {
		T obj = (T) objects[pd.idx];
		if (obj != null) {
			ensure((Long) obj).add(id);
		}
	}

	private LongArrayList ensure(long id) {
		if (!lookup.containsKey(id)) {
			lookup.put(id, new LongArrayList());
		}
		return lookup.get(id);
	}


	protected Object getValue(Long id) {
		return propertyStore.getValue(pd, id);
	}
}

package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Set;
import java.util.TreeSet;

import cc.alcina.framework.common.client.domain.DomainLookup;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.PdOperator;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongListIterator;

/*
 * Only indexes Longs!
 */
public class PropertyStoreLookup<T, H extends HasIdAndLocalId>
		extends DomainLookup<T, H> {
	protected PropertyStore propertyStore;

	private Long2ObjectOpenHashMap<LongArrayList> lookup = new Long2ObjectOpenHashMap<>();

	protected PdOperator pd;

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

	public void index(HasIdAndLocalId obj, boolean add) {
		long tgtIdx = CommonUtils.lv((Long) pd.read(obj));
		if (tgtIdx != 0) {
			if (add) {
				ensure(tgtIdx).add(obj.getId());
			} else {
				ensure(tgtIdx).remove(obj.getId());
			}
		}
	}

	public void initPds() {
		this.pd = propertyStore.getDescriptor(descriptor.propertyPath);
	}

	public void insert(Object[] row, long id) {
		long tgtIdx = CommonUtils.lv((Long) row[pd.idx]);
		if (tgtIdx != 0) {
			ensure(tgtIdx).add(id);
		}
	}

	private Set<Long> convertArr(LongArrayList longArrayList) {
		Set<Long> res = new TreeSet<Long>();
		LongListIterator itr = longArrayList.listIterator();
		while (itr.hasNext()) {
			res.add(itr.nextLong());
		}
		return res;
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

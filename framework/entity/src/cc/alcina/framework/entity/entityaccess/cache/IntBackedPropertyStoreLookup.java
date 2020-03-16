package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.CommonUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class IntBackedPropertyStoreLookup<T, H extends Entity>
		extends PropertyStoreLookup<T, H> {
	public static Set<Long>
			intCollectionToLongSet(IntCollection intCollection) {
		LongOpenHashSet res = new LongOpenHashSet();
		IntIterator itr = intCollection.iterator();
		while (itr.hasNext()) {
			res.add((long) itr.nextInt());
		}
		return res;
	}

	public static void retainAll(IntCollection intCollection,
			Set<Long> longCollection) {
		IntIterator itr = intCollection.iterator();
		for (; itr.hasNext();) {
			int i = itr.nextInt();
			Long l = Long.valueOf((long) i);
			if (!longCollection.contains(l)) {
				itr.remove();
			}
		}
	}

	private Int2ObjectOpenHashMap<IntArrayList> intBackedlookup = new Int2ObjectOpenHashMap<>();

	public IntBackedPropertyStoreLookup(
			PropertyStoreLookupDescriptor descriptor, PropertyStore store) {
		super(descriptor, store);
	}

	@Override
	public Set<Long> get(T k1) {
		if (k1 == null) {
			return null;
		}
		int id = ((Long) k1).intValue();
		if (intBackedlookup.containsKey(id)) {
			return convertArr(intBackedlookup.get(id));
		}
		return null;
	}

	@Override
	public void index(Entity obj, boolean add) {
		long tgtIdx = CommonUtils.lv((Long) pd.read(obj));
		if (tgtIdx != 0) {
			if (add) {
				ensure(tgtIdx).add((int) obj.getId());
			} else {
				ensure(tgtIdx).remove((int) obj.getId());
			}
		}
	}

	@Override
	public void insert(Object[] row, long id) {
		long tgtIdx = CommonUtils.lv((Long) row[pd.idx]);
		if (tgtIdx != 0) {
			ensure(tgtIdx).add((int) id);
		}
	}

	private Set<Long> convertArr(IntCollection intCollection) {
		return intCollectionToLongSet(intCollection);
	}

	private IntArrayList ensure(long id) {
		int iid = (int) id;
		if (!intBackedlookup.containsKey(iid)) {
			intBackedlookup.put(iid, new IntArrayList());
		}
		return intBackedlookup.get(iid);
	}

	@Override
	protected Object getValue(Long id) {
		return propertyStore.getValue(pd, id);
	}
}

package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class IntBackedPropertyStoreLookup<T, H extends HasIdAndLocalId>
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

	private Int2ObjectOpenHashMap<IntArrayList> lookup = new Int2ObjectOpenHashMap<>();

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
		if (lookup.containsKey(id)) {
			return convertArr(lookup.get(id));
		}
		return null;
	}

	public void index(HasIdAndLocalId obj, boolean add) {
		long tgtIdx = CommonUtils.lv((Long) pd.read(obj));
		if (tgtIdx != 0) {
			if (add) {
				ensure(tgtIdx).add((int) obj.getId());
			} else {
				ensure(tgtIdx).remove((int) obj.getId());
			}
		}
	}

	public void insert(ResultSet rs, long id) throws SQLException {
		long tgtIdx = rs.getLong(pd.idx + 1);
		if (tgtIdx != 0) {
			ensure(tgtIdx).add((int) id);
		}
	}

	private Set<Long> convertArr(IntCollection intCollection) {
		return intCollectionToLongSet(intCollection);
	}

	private IntArrayList ensure(long id) {
		int iid = (int) id;
		if (!lookup.containsKey(iid)) {
			lookup.put(iid, new IntArrayList());
		}
		return lookup.get(iid);
	}

	protected Object getValue(Long id) {
		return propertyStore.getValue(pd, id);
	}
}

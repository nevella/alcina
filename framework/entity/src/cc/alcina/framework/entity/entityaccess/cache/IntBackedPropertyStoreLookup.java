package cc.alcina.framework.entity.entityaccess.cache;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class IntBackedPropertyStoreLookup<T, H extends HasIdAndLocalId> extends
		PropertyStoreLookup<T, H> {
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

	private Set<Long> convertArr(IntArrayList intArrayList) {
		LongOpenHashSet res = new LongOpenHashSet();
		IntListIterator itr = intArrayList.listIterator();
		while (itr.hasNext()) {
			res.add((long) itr.nextInt());
		}
		return res;
	}

	public void insert(ResultSet rs, long id) throws SQLException {
		long tgtIdx = rs.getLong(pd.idx + 1);
		if (tgtIdx != 0) {
			ensure(tgtIdx).add((int) id);
		}
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

package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntObjectScatterMap;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;

public class IntBackedPropertyStoreLookup<T, H extends HasIdAndLocalId> extends
		PropertyStoreLookup<T, H> {
	private IntObjectScatterMap<IntArrayList> lookup = new IntObjectScatterMap<>();

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
		Set<Long> res = new TreeSet<Long>();
		for (IntCursor c : intArrayList) {
			res.add((long) c.value);
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

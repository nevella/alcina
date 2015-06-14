package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache.PdOperator;

import com.carrotsearch.hppc.IntIntScatterMap;
import com.carrotsearch.hppc.LongIntScatterMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;

public class IntBackedPropertyStore extends PropertyStore {
	IntIntScatterMap rowLookup;

	public Set<Long> getIds() {
		Set<Long> result = new LinkedHashSet<Long>();
		for (IntIntCursor cursor : rowLookup) {
			result.add((long) cursor.key);
		}
		return result;
	}

	protected void initRowLookup() {
		rowLookup = new IntIntScatterMap(getInitialSize());
	}

	public long getPrimitiveLongValue(PdOperator pd, int rowOffset) {
		if (rowOffset != -1) {
			return ((int[]) store.get(pd.idx))[rowOffset];
		}
		return 0;
	}

	protected int ensureRow(long id) {
		int iid = (int) id;
		if (!rowLookup.containsKey(iid)) {
			checkFull();
			rowLookup.put(iid, emptyRowIdx++);
		}
		return rowLookup.get(iid);
	}

	protected void writeLong(ResultSet rs, int rowIdx, int idx, Object pStore)
			throws SQLException {
		((long[]) pStore)[rowIdx] = (long) rs.getLong(idx + 1);
	}

	protected Object getLongArray() {
		return new int[tableSize];
	}

	@Override
	protected int getRowOffset(Long id) {
		if (id == null) {
			return -1;
		}
		int iid = id.intValue();
		if (rowLookup.containsKey(iid)) {
			return rowLookup.get(iid);
		}
		return -1;
	}

	@Override
	public Object getValue(PdOperator pd, Long id) {
		Object val = super.getValue(pd, id);
		if (val instanceof Integer) {
			return Long.valueOf(((Integer) val).longValue());
		} else {
			return val;
		}
	}
}

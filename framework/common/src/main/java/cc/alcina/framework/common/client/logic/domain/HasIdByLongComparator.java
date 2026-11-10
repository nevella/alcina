package cc.alcina.framework.common.client.logic.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;

public class HasIdByLongComparator implements Comparator<HasId> {
	private Map<Long, Integer> orderIndex = new HashMap<Long, Integer>();

	public HasIdByLongComparator(Collection<Long> orderBy) {
		int i = 1;
		for (Long l : orderBy) {
			orderIndex.put(l, i++);
		}
	}

	@Override
	public int compare(HasId o1, HasId o2) {
		int i1 = o1 == null || !orderIndex.containsKey(o1.getId()) ? 0
				: orderIndex.get(o1.getId());
		int i2 = o2 == null || !orderIndex.containsKey(o2.getId()) ? 0
				: orderIndex.get(o2.getId());
		return CommonUtils.compareInts(i1, i2);
	}
}

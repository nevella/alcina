package cc.alcina.framework.common.client.util;

import java.util.List;

import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHash;

public class HasEquivalenceHashMap<T extends HasEquivalence>
		extends Multimap<Integer, List<T>> {
	public void add(HasEquivalenceHash heh) {
		add(heh.equivalenceHash(), heh);
	}
}
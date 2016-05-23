package cc.alcina.framework.common.client.util;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;

import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHash;
import cc.alcina.framework.entity.J8Utils.ToMultimapCollector;

public class HasEquivalenceHashMap<T extends HasEquivalence>
		extends Multimap<Integer, List<T>> {
	public void add(HasEquivalenceHash heh) {
		add(heh.equivalenceHash(), heh);
	}

	public static <T extends HasEquivalence, K, U>
			Collector<T, ?, HasEquivalenceHashMap<T>>
			toKeyMultimap(Function<? super T, ? extends K> keyMapper) {
		return new ToMultimapCollector(keyMapper, t -> t,
				HasEquivalenceHashMap::new);
	}
}
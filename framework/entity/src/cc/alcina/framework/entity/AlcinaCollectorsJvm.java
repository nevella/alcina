package cc.alcina.framework.entity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Multimap;

public class AlcinaCollectorsJvm extends AlcinaCollectors {
	@Override
	public <T> Collector<Collection<T>, ?, Stream<T>> toItemStream0() {
		return J8Utils.toItemStream();
	}

	@Override
	public <T, K> Collector<T, ?, Map<K, T>>
			toKeyMap0(Function<? super T, ? extends K> keyMapper) {
		return J8Utils.toKeyMap(keyMapper);
	}

	@Override
	public <T, K, U> Collector<T, ?, Multimap<K, List<U>>>
			toKeyMultimap0(Function<? super T, ? extends K> keyMapper) {
		return J8Utils.toKeyMultimap(keyMapper);
	}

	@Override
	public <T, K, U> Collector<T, ?, Multimap<K, List<U>>> toMultimap0(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return J8Utils.toMultimap(keyMapper, valueMapper);
	}
}
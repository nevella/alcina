package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@RegistryLocation(registryPoint = AlcinaCollectors.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public abstract class AlcinaCollectors {
	public static <T> Collector<Collection<T>, ?, Stream<T>> toItemStream() {
		return Registry.impl(AlcinaCollectors.class).toItemStream0();
	}

	public static <T, K, U> Collector<T, ?, Multimap<K, List<U>>>
			toKeyMultimap(Function<? super T, ? extends K> keyMapper) {
		return Registry.impl(AlcinaCollectors.class).toKeyMultimap0(keyMapper);
	}

	public static <T, K, U> Collector<T, ?, Multimap<K, List<U>>> toMultimap(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper) {
		return Registry.impl(AlcinaCollectors.class).toMultimap0(keyMapper,
				valueMapper);
	}

	public abstract <T> Collector<Collection<T>, ?, Stream<T>> toItemStream0();

	public abstract <T, K, U> Collector<T, ?, Multimap<K, List<U>>>
			toKeyMultimap0(Function<? super T, ? extends K> keyMapper);

	public abstract <T, K, U> Collector<T, ?, Multimap<K, List<U>>> toMultimap0(
			Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper);
}

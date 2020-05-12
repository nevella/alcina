package cc.alcina.framework.common.client.domain.search;

import java.util.Optional;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.search.SearchDefinition;

public interface SearcherCollectionSource {
	<T> Optional<Stream<T>> getSourceStreamFor(Class<T> clazz,
			SearchDefinition def);

	@RegistryLocation(registryPoint = SearcherCollectionSource.class, implementationType = ImplementationType.SINGLETON)
	public static class SearcherCollectionSource_Domain
			implements SearcherCollectionSource {
		@Override
		public <T> Optional<Stream<T>> getSourceStreamFor(Class<T> clazz,
				SearchDefinition def) {
			return Optional.empty();
		}
	}
}
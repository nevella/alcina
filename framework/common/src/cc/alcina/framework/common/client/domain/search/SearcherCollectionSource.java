package cc.alcina.framework.common.client.domain.search;

import java.util.Optional;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.search.SearchDefinition;

public interface SearcherCollectionSource {
	default void beforeQuery(Class clazz, BindableSearchDefinition def) {
	}

	<T> Optional<Stream<T>> getSourceStreamFor(Class<T> clazz,
			SearchDefinition def);

	@RegistryLocation(registryPoint = SearcherCollectionSource.class, implementationType = ImplementationType.SINGLETON)
	@Registration.Singleton(SearcherCollectionSource.class)
	public static class SearcherCollectionSource_Domain
			implements SearcherCollectionSource {
		@Override
		public <T> Optional<Stream<T>> getSourceStreamFor(Class<T> clazz,
				SearchDefinition def) {
			return Optional.empty();
		}
	}
}

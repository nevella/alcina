package cc.alcina.framework.common.client.domain.search;

import java.util.Collection;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.search.SearchDefinition;

public interface SearcherCollectionSource {
	<T> Collection<T> getCollectionFor(Class<T> clazz, SearchDefinition def);

	public static class SearcherCollectionSource_Domain
			implements SearcherCollectionSource {
		@Override
		public <T> Collection<T> getCollectionFor(Class<T> clazz,
				SearchDefinition def) {
			return (Collection) Domain.values((Class) clazz);
		}
	}
}
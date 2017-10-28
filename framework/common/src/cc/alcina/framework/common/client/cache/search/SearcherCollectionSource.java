package cc.alcina.framework.common.client.cache.search;

import java.util.Collection;

import cc.alcina.framework.common.client.search.SearchDefinition;

public interface SearcherCollectionSource {
	<T> Collection<T> getCollectionFor(Class<T> clazz, SearchDefinition def);
}
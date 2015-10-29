package cc.alcina.framework.common.client.cache.search;

import java.util.Collection;

public interface SearcherCollectionSource {
	<T> Collection<T> getCollectionFor(Class<T> clazz);
}
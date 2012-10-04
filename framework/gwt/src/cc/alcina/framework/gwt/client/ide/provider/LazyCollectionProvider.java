package cc.alcina.framework.gwt.client.ide.provider;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface LazyCollectionProvider<T> extends CollectionProvider {

	public abstract boolean filter(String filterText);

	public abstract String getTitle();

	public abstract boolean containsObject(Object userObject);

	public abstract int getMinFilterableLength();

	public abstract Collection getObjectsRecursive(List list);

	public abstract Set<? extends LazyCollectionProvider<T>> getChildProviders();
}
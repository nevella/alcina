/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.ide.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.gwt.client.widget.VisualFilterable.HasSatisfiesFilter;

/**
 * 
 * @author Nick Reddel
 */
@SuppressWarnings("unchecked")
public class UmbrellaCollectionProviderMultiplexer<T>
		implements CollectionModificationListener {
	protected final Collection<T> collection;

	protected final UmbrellaProvider<T> umbrellaProvider;

	private Map<String, UmbrellaCollectionProvider> items = new LinkedHashMap<String, UmbrellaCollectionProvider>();

	private final Class<? extends T> baseClass;

	private final int minFilterableDepth;

	private CollectionFilter<T> collectionFilter;

	public UmbrellaCollectionProviderMultiplexer(Collection<T> colln,
			Class<? extends T> baseClass, UmbrellaProvider<T> umbrellaProvider,
			CollectionFilter<T> collectionFilter, int minFilterableLength) {
		this.collection = colln;
		this.baseClass = baseClass;
		this.umbrellaProvider = umbrellaProvider;
		this.collectionFilter = collectionFilter;
		this.minFilterableDepth = minFilterableLength;
		remap(null);
	}

	// chained through to the node
	public void collectionModification(CollectionModificationEvent evt) {
		CollectionModificationEvent simpleEvent = new CollectionModificationEvent(
				evt.getSource());
		remap(simpleEvent);
	}

	public UmbrellaCollectionProvider createProviderChild(String key) {
		return new UmbrellaCollectionProviderChildString(key);
	}

	public Collection getCollection() {
		return null;
	}

	public CollectionFilter<T> getCollectionFilter() {
		return this.collectionFilter;
	}

	public UmbrellaCollectionProvider getRootSubprovider() {
		return items.get("");
	}

	public void onDetach() {
		TransformManager.get().removeCollectionModificationListener(this);
	}

	public void setCollectionFilter(CollectionFilter<T> collectionFilter) {
		this.collectionFilter = collectionFilter;
		collectionModification(new CollectionModificationEvent(this));
	}

	private void remap(CollectionModificationEvent simpleEvent) {
		umbrellaProvider.forCollection(collection, collectionFilter);
		Stack<UmbrellaCollectionProvider> providerStack = new Stack<UmbrellaCollectionProvider>();
		String key = "";
		for (UmbrellaCollectionProvider existing : items.values()) {
			existing.childIterator = umbrellaProvider
					.getUmbrellaNames(existing.key).iterator();
		}
		UmbrellaCollectionProvider root = items.get(key);
		if (root == null) {
			root = createProviderChild(key);
			items.put(key, root);
		}
		providerStack.push(root);
		while (!providerStack.isEmpty()) {
			UmbrellaCollectionProvider current = providerStack.peek();
			Iterator<String> itr = current.childIterator;
			if (!itr.hasNext()) {
				providerStack.pop();
				continue;
			}
			key = itr.next();
			List<T> objects = umbrellaProvider.getUmbrellaObjects(key);
			UmbrellaCollectionProvider child = items.get(key);
			if (child != null) {
				if (simpleEvent != null && !objects.equals(child.objects)) {
					child.objects = objects;
					child.fireCollectionModificationEvent(simpleEvent);
				}
			} else {
				child = createProviderChild(key);
				items.put(key, child);
			}
			current.childProviders.add(child);
			providerStack.push(child);
		}
	}

	public class UmbrellaCollectionProvider implements
			LazyCollectionProvider<T>, Comparable<UmbrellaCollectionProvider> {
		private CollectionModificationSupport collectionModificationSupport = new CollectionModificationSupport();

		protected final String key;

		Iterator<String> childIterator;

		List objects;

		Set<UmbrellaCollectionProvider> childProviders = new LinkedHashSet<UmbrellaCollectionProvider>();

		private List filteredCollection;

		public UmbrellaCollectionProvider(String key) {
			this.key = key;
			childIterator = umbrellaProvider.getUmbrellaNames(key).iterator();
			objects = umbrellaProvider.getUmbrellaObjects(key);
		}

		@Override
		public void addCollectionModificationListener(
				CollectionModificationListener listener) {
			this.collectionModificationSupport
					.addCollectionModificationListener(listener);
		}

		@Override
		public int compareTo(UmbrellaCollectionProvider o) {
			return key.compareTo(o.key);
		}

		@Override
		public boolean containsObject(Object userObject) {
			if (objects.contains(userObject)) {
				return true;
			}
			for (LazyCollectionProvider<T> provider : getChildProviders()) {
				if (provider.containsObject(userObject)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof UmbrellaCollectionProviderMultiplexer.UmbrellaCollectionProvider) {
				return key.equals(((UmbrellaCollectionProvider) obj).key);
			}
			return false;
		}

		@Override
		public boolean filter(String filterText) {
			if (filterText.isEmpty()) {
				filteredCollection = null;
				for (LazyCollectionProvider<T> provider : getChildProviders()) {
					provider.filter(filterText);
				}
				return true;
			}
			filteredCollection = new ArrayList();
			for (LazyCollectionProvider<T> provider : getChildProviders()) {
				if (provider.filter(filterText)) {
					filteredCollection.add(provider);
				}
			}
			TextProvider tp = TextProvider.get();
			for (Object object : objects) {
				if (Registry.impl(HasSatisfiesFilter.class, object.getClass())
						.satisfiesFilter(object, filterText)) {
					filteredCollection.add(object);
				}
			}
			return filteredCollection.size() > 0;
		}

		public void fireCollectionModificationEvent(
				CollectionModificationEvent event) {
			this.collectionModificationSupport
					.fireCollectionModificationEvent(event);
		}

		@Override
		public Set<UmbrellaCollectionProvider> getChildProviders() {
			return this.childProviders;
		}

		@Override
		public Collection getCollection() {
			if (filteredCollection == null) {
				resetFilteredCollection();
			}
			return filteredCollection;
		}

		@Override
		public Class getCollectionMemberClass() {
			return baseClass;
		}

		@Override
		public int getCollectionSize() {
			return childProviders.size() + objects.size();
		}

		@Override
		public int getMinFilterableLength() {
			return minFilterableDepth;
		}

		public List getObjects() {
			return this.objects;
		}

		@Override
		public Collection getObjectsRecursive(List list) {
			list = list == null ? new ArrayList() : list;
			for (Object o : getCollection()) {
				if (o instanceof UmbrellaCollectionProviderMultiplexer.UmbrellaCollectionProvider) {
					((UmbrellaCollectionProviderMultiplexer.UmbrellaCollectionProvider) o)
							.getObjectsRecursive(list);
				} else {
					list.add(o);
				}
			}
			return list;
		}

		@Override
		public String getTitle() {
			return key;
		}

		@Override
		public int hashCode() {
			return key.hashCode();
		}

		@Override
		public void onDetach() {
		}

		@Override
		public void removeCollectionModificationListener(
				CollectionModificationListener listener) {
			this.collectionModificationSupport
					.removeCollectionModificationListener(listener);
		}

		private void resetFilteredCollection() {
			filteredCollection = new ArrayList();
			filteredCollection.addAll(childProviders);
			filteredCollection.addAll(objects);
		}
	}

	protected class UmbrellaCollectionProviderChildString
			extends UmbrellaCollectionProvider {
		public UmbrellaCollectionProviderChildString(String key) {
			super(key);
		}
	}
}

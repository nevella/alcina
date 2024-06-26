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
package cc.alcina.framework.gwt.client.ide.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.PropertyCollectionProvider;

/**
 *
 * @author Nick Reddel
 */
public class CollectionRenderingSupport
		implements CollectionModificationListener, ProvidesParenting {
	public static boolean REDRAW_CHILDREN_ON_ORDER_CHANGE = false;

	private boolean volatileOrder;

	private final TreeOrItem item;

	private CollectionProvider collectionProvider;

	private Comparator comparator;

	private boolean dirty = true;

	private boolean lazyRefresh = false;

	public CollectionRenderingSupport(Tree tree) {
		this.item = new TreeOrItemTree(tree);
	}

	public CollectionRenderingSupport(TreeItem item) {
		this.item = new TreeOrItemTreeItem(item);
	}

	public void collectionModification(CollectionModificationEvent evt) {
		dirty = true;
		refreshChildren(true);
	}

	protected Collection getCollection() {
		return collectionProvider.getCollection();
	}

	public CollectionProvider getCollectionProvider() {
		return this.collectionProvider;
	}

	public Comparator getComparator() {
		return comparator;
	}

	public Class getListenedClass() {
		return getCollectionProvider().getCollectionMemberClass();
	}

	protected NodeFactory getNodeFactory() {
		NodeFactory nodeFactory = item.getNodeFactory();
		return nodeFactory != null ? nodeFactory : NodeFactory.get();
	}

	public PropertyCollectionProvider getPropertyCollectionProvider() {
		if (getCollectionProvider() instanceof PropertyCollectionProvider) {
			return (PropertyCollectionProvider) getCollectionProvider();
		}
		return null;
	}

	public Object getUserObject() {
		return collectionProvider;
	}

	public Collection getVisibleItemObjects() {
		ArrayList l = new ArrayList();
		for (int i = 0; i < item.getChildCount(); i++) {
			TreeItem child = item.getChild(i);
			if (child.isVisible() && child instanceof DomainNode) {
				DomainNode dn = (DomainNode) child;
				l.add(dn.getUserObject());
			}
		}
		return l;
	}

	public boolean isDirty() {
		return this.dirty;
	}

	public boolean isLazyRefresh() {
		return lazyRefresh;
	}

	/**
	 * Slightly misnamed - basically forces a complete redraw on any refresh() -
	 * i.e. any collection change, or a direct call
	 */
	public boolean isVolatileOrder() {
		return volatileOrder;
	}

	/*
	 * this could theoretically by O(n^2)...unlikely in the extreme but
	 */
	private int[] nextCommonObject(List l1, List l2, int i1, int i2) {
		int[] res = new int[2];
		int maxCheckIndex = Math.min(l1.size(), l2.size());
		boolean matched = false;
		while (i1 < maxCheckIndex && i2 < maxCheckIndex) {
			for (int i = 0; i1 + i < maxCheckIndex; i++) {
				// l1[n] <> l2[n+i]
				if (i2 + i < l2.size()) {
					if (l1.get(i1).equals(l2.get(i2 + i))) {
						i2 = i2 + i;
						matched = true;
						break;
					}
				}
				// other way
				if (i1 + i < l1.size()) {
					if (l1.get(i1 + i).equals(l2.get(i2))) {
						i1 = i1 + i;
						matched = true;
						break;
					}
				}
			}
			if (matched) {
				break;
			}
			i1++;
			i2++;
		}
		if (!matched) {
			i1 = l1.size();
			i2 = l2.size();
		}
		res[0] = i1;
		res[1] = i2;
		return res;
	}

	public void onDetach() {
		if (collectionProvider != null) {
			collectionProvider.removeCollectionModificationListener(this);
			collectionProvider.onDetach();
		}
	}

	public void refreshChildren(boolean lazy) {
		if (lazy && lazyRefresh && !item.getState()) {
			return;
		}
		Map<Object, TreeItem> existingObjects = new LinkedHashMap<Object, TreeItem>();
		for (int i = 0; i < item.getChildCount(); i++) {
			TreeItem child = item.getChild(i);
			if (child != null) {
				existingObjects.put(child.getUserObject(), child);
			}
		}
		Collection objects = getCollection();
		if (objects == null || objects.isEmpty()) {
			if (!existingObjects.isEmpty()) {
				item.removeItems();
			}
			return;
		}
		if (volatileOrder) {
			if (!existingObjects.isEmpty()) {
				item.removeItems();
				existingObjects.clear();
			}
		}
		List currentObjects = new ArrayList(objects);
		Object o = currentObjects.get(0);
		if (comparator != null) {
			Collections.sort(currentObjects, comparator);
		} else if (o instanceof Comparable) {
			Collections.sort(currentObjects);
		}
		if ((o instanceof Comparable)
				&& CollectionRenderingSupport.REDRAW_CHILDREN_ON_ORDER_CHANGE
				&& existingObjects.size() == currentObjects.size()) {
			List existingOrderedObjects = new ArrayList(
					existingObjects.keySet());
			List currentOrderedObjects = new ArrayList(currentObjects);
			Collections.sort(existingOrderedObjects);
			Collections.sort(currentOrderedObjects);
			if (!currentOrderedObjects.equals(existingOrderedObjects)) {
				existingObjects.clear();
				this.item.removeItems();
			}
		}
		int i1 = 0, i2 = 0;
		// find next common object, run the indexes up to it. if -1, run indexes
		// to end
		List existingList = new ArrayList(existingObjects.keySet());
		while (i1 < existingObjects.size() || i2 < currentObjects.size()) {
			int[] nextCommon = nextCommonObject(existingList, currentObjects,
					i1, i2);
			for (; i1 < nextCommon[0]; i1++) {
				this.item.removeItem(existingObjects.get(existingList.get(i1)));
			}
			for (; i2 < nextCommon[1]; i2++) {
				// if there were an "insertItem" on the tree API, this'd be
				// where we'd use it...
				// well..there is...but anyway (did someone slip that in?) -
				// anyway, adding at end is sometimes
				// nicer UI-wise
				item.addItem(getNodeFactory()
						.getNodeForObject(currentObjects.get(i2)));
			}
			i1++;
			i2++;
		}
	}

	public void refreshChildrenIfDirty() {
		if (dirty) {
			dirty = false;
			refreshChildren(false);
		}
	}

	public void removeItem(TreeItem item) {
		if (item instanceof DomainNode) {
			((DomainNode) item).removeListeners();
		}
	}

	public void setCollectionProvider(CollectionProvider collectionProvider) {
		this.collectionProvider = collectionProvider;
		dirty = true;
		if (collectionProvider != null) {
			refreshChildren(true);
			collectionProvider.addCollectionModificationListener(this);
		}
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public void setLazyRefresh(boolean lazyRefresh) {
		this.lazyRefresh = lazyRefresh;
	}

	public void setVolatileOrder(boolean volatileOrder) {
		this.volatileOrder = volatileOrder;
	}
}

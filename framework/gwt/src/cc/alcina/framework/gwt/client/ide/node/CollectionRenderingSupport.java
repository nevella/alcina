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

import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.PropertyCollectionProvider;

import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.totsp.gwittir.client.beans.Bindable;
@SuppressWarnings("unchecked")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class CollectionRenderingSupport implements
		CollectionModificationListener, ProvidesParenting {
	public static boolean REDRAW_CHILDREN_ON_ORDER_CHANGE = false;

	private boolean volatileOrder;

	private final TreeOrItem item;

	private CollectionProvider collectionProvider;

	private Comparator comparator;

	public CollectionRenderingSupport(TreeItem item) {
		this.item = new TreeOrItemTreeItem(item);
	}

	public CollectionRenderingSupport(Tree tree) {
		this.item = new TreeOrItemTree(tree);
	}

	public void setCollectionProvider(CollectionProvider collectionProvider) {
		this.collectionProvider = collectionProvider;
		if (collectionProvider != null) {
			refreshChildren();
			collectionProvider.addCollectionModificationListener(this);
		}
	}

	public Object getUserObject() {
		return collectionProvider;
	}

	public void collectionModification(CollectionModificationEvent evt) {
		refreshChildren();
	}

	public CollectionProvider getCollectionProvider() {
		return this.collectionProvider;
	}

	public Class getListenedClass() {
		return getCollectionProvider().getCollectionClass();
	}

	public PropertyCollectionProvider getPropertyCollectionProvider() {
		if (getCollectionProvider() instanceof PropertyCollectionProvider) {
			return (PropertyCollectionProvider) getCollectionProvider();
		}
		return null;
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

	public void onDetach() {
		if(collectionProvider!=null){
			collectionProvider.removeCollectionModificationListener(this);
			collectionProvider.onDetach();
		}
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

	public void refreshChildren() {
		Collection objects = collectionProvider.getCollection();
		if (objects == null || objects.isEmpty()) {
			item.removeItems();
			return;
		}
		if (volatileOrder) {
			item.removeItems();
		}
		Map<Object, TreeItem> existingObjects = new LinkedHashMap<Object, TreeItem>();
		List existingList = new ArrayList();
		List currentObjects = new ArrayList(objects);
		for (int i = 0; i < item.getChildCount(); i++) {
			TreeItem child = item.getChild(i);
			existingObjects.put(child.getUserObject(), child);
			existingList.add(child.getUserObject());
		}
		Object o = currentObjects.get(0);
		if (comparator != null) {
			Collections.sort(currentObjects, comparator);
		} else if (o instanceof Comparable) {
			Collections.sort(currentObjects);
		}
		if (CollectionRenderingSupport.REDRAW_CHILDREN_ON_ORDER_CHANGE
				&& existingObjects.size() == currentObjects.size()) {
			List existingOrderedObjects = new ArrayList(existingObjects
					.keySet());
			List currentOrderedObjects = new ArrayList(currentObjects);
			Collections.sort(existingOrderedObjects);
			Collections.sort(currentOrderedObjects);
			if (!currentOrderedObjects.equals(existingOrderedObjects)) {
				existingObjects.clear();
			}
		}
		int i1 = 0, i2 = 0;
		// find next common object, run the indexes up to it. if -1, run indexes
		// to end
		while (i1 < existingList.size() || i2 < currentObjects.size()) {
			int[] nextCommon = nextCommonObject(existingList, currentObjects,
					i1, i2);
			for (; i1 < nextCommon[0]; i1++) {
				this.item.removeItem(existingObjects.get(existingList.get(i1)));
			}
			for (; i2 < nextCommon[1]; i2++) {
				//if there were an "insertItem" on the tree API, this'd be where we'd use it...
				item.addItem(NodeFactory.get().getNodeForDomainObject(
						(Bindable) currentObjects.get(i2)));
			}
			i1++;
			i2++;
		}
	}

	public void removeItem(TreeItem item) {
		((DomainNode) item).removeListeners();
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	public Comparator getComparator() {
		return comparator;
	}

	public void setVolatileOrder(boolean volatileOrder) {
		this.volatileOrder = volatileOrder;
	}

	public boolean isVolatileOrder() {
		return volatileOrder;
	}

	public interface TreeOrItem {
		public void addItem(TreeItem item);

		public void removeItem(TreeItem item);

		public void removeItems();

		public TreeItem getChild(int index);

		public int getChildCount();
	}

	public static class TreeOrItemTreeItem implements TreeOrItem {
		private final TreeItem item;

		public TreeItem getChild(int index) {
			return this.item.getChild(index);
		}

		public int getChildCount() {
			return this.item.getChildCount();
		}

		public void addItem(TreeItem item) {
			this.item.addItem(item);
		}

		public void removeItem(TreeItem item) {
			this.item.removeItem(item);
		}

		public void removeItems() {
			this.item.removeItems();
		}

		public TreeOrItemTreeItem(TreeItem item) {
			this.item = item;
		}
	}

	public static class TreeOrItemTree implements TreeOrItem {
		private final Tree tree;

		public TreeItem getChild(int index) {
			return this.tree.getItem(index);
		}

		public int getChildCount() {
			return this.tree.getItemCount();
		}

		public void addItem(TreeItem item) {
			this.tree.addItem(item);
		}

		public void removeItem(TreeItem item) {
			this.tree.removeItem(item);
		}

		public void removeItems() {
			this.tree.removeItems();
		}

		public TreeOrItemTree(Tree tree) {
			this.tree = tree;
		}
	}
}

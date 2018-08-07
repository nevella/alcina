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

import java.util.Collection;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.TreeItem;

import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.LazyCollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.PropertyCollectionProvider;
import cc.alcina.framework.gwt.client.widget.VisualFilterable;

/**
 * 
 * @author Nick Reddel
 */
public class UmbrellaProviderNode extends ContainerNode
		implements CollectionModificationListener, ProvidesParenting,
		HasVisibleCollection {
	private TreeItem dummy = new TreeItem("loading...");

	private CollectionRenderingSupport support = null;

	private final LazyCollectionProvider provider;

	public UmbrellaProviderNode(LazyCollectionProvider provider, String title,
			ImageResource imageResource, NodeFactory nodeFactory) {
		super(title, imageResource, nodeFactory);
		this.provider = provider;
		setCollectionProvider(provider);
		title = title == null ? provider.getTitle() : title;
		this.title = title;
		dummy();
	}

	@Override
	public void collectionModification(CollectionModificationEvent evt) {
		this.support.collectionModification(evt);
	}

	@Override
	public boolean filter(String filterText, boolean enforceVisible) {
		boolean top = getParentItem() == null;
		if (filterText.length() < provider.getMinFilterableLength()) {
			TreeItem selectedItem = getTree().getSelectedItem();
			Object selectedObject = selectedItem == null ? null
					: selectedItem.getUserObject();
			provider.filter("");
			removeItems();
			support.setDirty(true);
			if (selectedObject != null) {
				openToObject(selectedObject);
			} else {
				setState(true, false);
			}
			return true;
		} else {
			provider.filter(filterText);
		}
		boolean show = !provider.getCollection().isEmpty();
		if (show) {
			support.setDirty(true);
			setState(true, false);
			filterChildren(filterText);
		} else {
			if (top) {
				filterChildren(filterText);
			} else {
				dummy();
			}
		}
		setVisible(show || top);
		return show;
	}

	@Override
	public Class getCollectionMemberClass() {
		return provider.getCollectionMemberClass();
	}

	public CollectionProvider getCollectionProvider() {
		return this.support.getCollectionProvider();
	}

	public Class getListenedClass() {
		return this.support.getListenedClass();
	}

	@Override
	public PropertyCollectionProvider getPropertyCollectionProvider() {
		return this.support.getPropertyCollectionProvider();
	}

	@Override
	public Object getUserObject() {
		return this.support.getUserObject();
	}

	@Override
	public Collection getVisibleCollection() {
		return provider.getObjectsRecursive(null);
	}

	public Collection getVisibleItemObjects() {
		return this.support.getVisibleItemObjects();
	}

	public boolean hasChildren() {
		return provider.getCollectionSize() != 0;
	}

	@Override
	public void onDetach() {
		if (support != null) {
			support.onDetach();
		}
		super.onDetach();
	}

	public void refreshChildren() {
		this.support.refreshChildren(false);
	}

	@Override
	public void removeItem(TreeItem item) {
		super.removeItem(item);
		support.removeItem(item);
	}

	@Override
	public void setCollectionProvider(CollectionProvider collectionProvider) {
		support = new CollectionRenderingSupport(this);
		support.setLazyRefresh(true);
		support.setCollectionProvider(collectionProvider);
	}

	@Override
	public void setState(boolean open, boolean fireEvents) {
		if (open) {
			if (dummy.getParentItem() == this) {
				support.setDirty(true);
				removeItem(dummy);
			}
			support.refreshChildrenIfDirty();
		}
		super.setState(open, fireEvents);
	}

	private void dummy() {
		if (provider.getChildProviders().isEmpty()) {
			removeItems();
			addItem(dummy);
		}
	}

	protected void filterChildren(String filterText) {
		for (int i = 0; i < getChildCount(); i++) {
			TreeItem child = getChild(i);
			if (child instanceof VisualFilterable) {
				VisualFilterable vf = (VisualFilterable) child;
				vf.filter(filterText);
			}
		}
	}

	@Override
	protected String imageItemHTML(AbstractImagePrototype imageProto,
			String title) {
		return imageProto.getHTML() + " " + title;
	}

	// true == finished
	protected boolean openToObject(Object userObject) {
		setState(true, false);
		for (int i = 0; i < getChildCount(); i++) {
			TreeItem child = getChild(i);
			Object childObject = child.getUserObject();
			if (childObject == userObject) {
				getTree().setSelectedItem(child);
				getTree().ensureSelectedItemVisible();
				DOM.scrollIntoView(child.getElement());
				return true;
			}
			if (child instanceof UmbrellaProviderNode) {
				UmbrellaProviderNode upn = (UmbrellaProviderNode) child;
				if (upn.provider.containsObject(userObject)) {
					return upn.openToObject(userObject);
				}
			}
		}
		return false;
	}
}

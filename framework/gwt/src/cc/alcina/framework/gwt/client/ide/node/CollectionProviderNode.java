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
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.TreeItem;

import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.PropertyCollectionProvider;
import cc.alcina.framework.gwt.client.ide.widget.DataTree;

/**
 *
 * @author Nick Reddel
 */
public class CollectionProviderNode extends ContainerNode
		implements CollectionModificationListener, ProvidesParenting,
		HasVisibleCollection {
	private CollectionRenderingSupport support = null;

	public CollectionProviderNode(CollectionProvider collectionProvider,
			String title, ImageResource imageResource) {
		this(collectionProvider, title, imageResource, false, null);
	}

	public CollectionProviderNode(CollectionProvider collectionProvider,
			String title, ImageResource imageResource, boolean volatileOrder,
			NodeFactory nodeFactory) {
		super(title, imageResource, nodeFactory);
		setCollectionProvider(collectionProvider);
		this.support.setVolatileOrder(volatileOrder);
	}

	@Override
	public void collectionModification(CollectionModificationEvent evt) {
		this.support.collectionModification(evt);
	}

	@Override
	public Class getCollectionMemberClass() {
		return getCollectionProvider().getCollectionMemberClass();
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
		return this.support.getVisibleItemObjects();
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
		support.setCollectionProvider(collectionProvider);
	}

	@Override
	protected String imageItemHTML(AbstractImagePrototype imageProto,
			String title) {
		if (((DataTree) getTree()).isUseNodeImages()) {
			return imageProto.getHTML() + " " + title;
		} else {
			return title;
		}
	}
}

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

import com.google.gwt.user.client.ui.TreeItem;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.PropertyCollectionProvider;

/**
 *
 * @author Nick Reddel
 */

 public class DomainCollectionProviderNode<T extends SourcesPropertyChangeEvents> extends
		DomainNode<T> implements CollectionModificationListener,
		ProvidesParenting {
	protected CollectionRenderingSupport support = null;

	public DomainCollectionProviderNode(T object) {
		this(object,null);
	}
	public DomainCollectionProviderNode(T object,NodeFactory factory) {
		super(object,factory);
	}

	public void removeItem(TreeItem item) {
		super.removeItem(item);
		support.removeItem(item);
	}

	@Override
	public void onDetach() {
		if (support!=null){
			support.onDetach();
		}
		super.onDetach();
	}

	public void collectionModification(CollectionModificationEvent evt) {
		this.support.collectionModification(evt);
	}

	public CollectionProvider getCollectionProvider() {
		return this.support.getCollectionProvider();
	}

	public Class getListenedClass() {
		return this.support.getListenedClass();
	}

	public PropertyCollectionProvider getPropertyCollectionProvider() {
		return this.support.getPropertyCollectionProvider();
	}

	public Collection getVisibleItemObjects() {
		return this.support.getVisibleItemObjects();
	}

	public void refreshChildren() {
		this.support.refreshChildren(false);
	}

	public void setCollectionProvider(CollectionProvider collectionProvider) {
		support = new CollectionRenderingSupport(this);
		support.setCollectionProvider(collectionProvider);
	}
}

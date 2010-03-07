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

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class SimpleCollectionProvider<E> implements CollectionProvider<E>,
		CollectionModificationListener {
	private final Collection<E> colln;

	private final Class<? extends E> baseClass;

	private CollectionModificationSupport collectionModificationSupport = new CollectionModificationSupport();

	private CollectionFilter<E> filter;

	public void setFilter(CollectionFilter<E> filter) {
		this.filter = filter;
		collectionModification(new CollectionModificationEvent(this));
	}

	public SimpleCollectionProvider(Collection<E> colln,
			Class<? extends E> baseClass) {
		this(colln, baseClass, null);
	}

	public SimpleCollectionProvider(Collection<E> colln,
			Class<? extends E> baseClass, CollectionFilter<E> filter) {
		this.colln = colln;
		this.baseClass = baseClass;
		this.filter = filter;
	}

	public CollectionFilter<E> getFilter() {
		return this.filter;
	}

	public Collection<E> getCollection() {
		if (filter == null) {
			return colln;
		}
		ArrayList<E> l = new ArrayList<E>();
		for (E e : colln) {
			if (filter.allow(e)) {
				l.add(e);
			}
		}
		return l;
	}

	public Class<? extends E> getCollectionClass() {
		return this.baseClass;
	}

	public void addCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.addCollectionModificationListener(listener);
	}

	public void removeCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.removeCollectionModificationListener(listener);
	}

	public void onDetach() {
		TransformManager.get().removeCollectionModificationListener(this);
	}

	// chained through to the node
	public void collectionModification(CollectionModificationEvent evt) {
		CollectionModificationEvent simpleEvent = new CollectionModificationEvent(
				evt.getSource());
		this.collectionModificationSupport
				.fireCollectionModificationEvent(simpleEvent);
	}
}

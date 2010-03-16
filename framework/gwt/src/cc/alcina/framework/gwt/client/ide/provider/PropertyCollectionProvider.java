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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;

import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.ClientPropertyReflector;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

/**
 *
 * @author Nick Reddel
 */

 public class PropertyCollectionProvider<E> implements CollectionProvider<E>,
		PropertyChangeListener {
	private CollectionFilter<E> filter;

	private final SourcesPropertyChangeEvents domainObject;

	private final ClientPropertyReflector propertyReflector;

	public void setFilter(CollectionFilter<E> filter) {
		this.filter = filter;
	}

	public PropertyCollectionProvider(SourcesPropertyChangeEvents domainObject,
			ClientPropertyReflector propertyReflector) {
		this.domainObject = domainObject;
		this.propertyReflector = propertyReflector;
		domainObject.addPropertyChangeListener(propertyReflector
				.getPropertyName(), this);
	}

	public CollectionFilter<E> getFilter() {
		return this.filter;
	}
	@SuppressWarnings("unchecked")
	public Collection<E> getCollection() {
		Collection<E> colln = (Collection) GwittirBridge.get()
				.getPropertyValue(getDomainObject(),
						getPropertyReflector().getPropertyName());
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

	private CollectionModificationSupport collectionModificationSupport = new CollectionModificationSupport();
	@SuppressWarnings("unchecked")
	public Class<? extends E> getCollectionClass() {
		return getPropertyReflector().getAnnotation(Association.class)
				.implementationClass();
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
		getDomainObject().removePropertyChangeListener(
				getPropertyReflector().getPropertyName(), this);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		this.collectionModificationSupport
				.fireCollectionModificationEvent(new CollectionModificationEvent(
						getDomainObject()));
	}

	public SourcesPropertyChangeEvents getDomainObject() {
		return domainObject;
	}

	public ClientPropertyReflector getPropertyReflector() {
		return propertyReflector;
	}
}

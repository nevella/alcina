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
import java.util.function.Predicate;

import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

/**
 * 
 * @author Nick Reddel
 */
public class PropertyCollectionProvider<E>
		implements CollectionProvider<E>, PropertyChangeListener {
	private Predicate<E> filter;

	private final SourcesPropertyChangeEvents domainObject;

	private final Property property;

	private CollectionModificationSupport collectionModificationSupport = new CollectionModificationSupport();

	public PropertyCollectionProvider(SourcesPropertyChangeEvents domainObject,
			Property property) {
		this.domainObject = domainObject;
		this.property = property;
		domainObject.addPropertyChangeListener(property.getName(), this);
		Display display = property.annotation(Display.class);
		if (display != null) {
			Class filterClass = display.filterClass();
			if (filterClass != null && filterClass != Void.class) {
				filter = (Predicate<E>) Reflections.newInstance(filterClass);
			}
		}
	}

	@Override
	public void addCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.addCollectionModificationListener(listener);
	}

	@Override
	public Collection<E> getCollection() {
		Collection<E> colln = (Collection<E>) property.get(domainObject);
		if (filter == null) {
			return colln;
		}
		ArrayList<E> l = new ArrayList<E>();
		for (E e : colln) {
			if (filter.test(e)) {
				l.add(e);
			}
		}
		return l;
	}

	@Override
	public Class<? extends E> getCollectionMemberClass() {
		return getProperty().annotation(Association.class)
				.implementationClass();
	}

	@Override
	public int getCollectionSize() {
		return getCollection().size();
	}

	public SourcesPropertyChangeEvents getDomainObject() {
		return domainObject;
	}

	public Predicate<E> getFilter() {
		return this.filter;
	}

	public Property getProperty() {
		return property;
	}

	@Override
	public void onDetach() {
		getDomainObject().removePropertyChangeListener(
				getProperty().getName(), this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		this.collectionModificationSupport.fireCollectionModificationEvent(
				new CollectionModificationEvent(getDomainObject()));
	}

	@Override
	public void removeCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.removeCollectionModificationListener(listener);
	}

	public void setFilter(Predicate<E> filter) {
		this.filter = filter;
	}
}

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
import java.util.Comparator;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationEvent;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationListener;
import cc.alcina.framework.common.client.logic.domaintransform.CollectionModification.CollectionModificationSupport;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

/**
 *
 * @author Nick Reddel
 */
public class SimpleCollectionProvider<E>
		implements CollectionProvider<E>, CollectionModificationListener {
	private final Collection<E> colln;

	private final Class<? extends E> baseClass;

	private CollectionModificationSupport collectionModificationSupport = new CollectionModificationSupport();

	private Predicate<E> predicate;

	private Comparator<E> comparator;

	public SimpleCollectionProvider(Collection<E> colln,
			Class<? extends E> baseClass) {
		this(colln, baseClass, null);
	}

	public SimpleCollectionProvider(Collection<E> colln,
			Class<? extends E> baseClass, Predicate<E> filter) {
		this.colln = colln;
		this.baseClass = baseClass;
		this.predicate = filter;
	}

	@Override
	public void addCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.addCollectionModificationListener(listener);
	}

	// chained through to the node
	@Override
	public void collectionModification(CollectionModificationEvent evt) {
		CollectionModificationEvent simpleEvent = new CollectionModificationEvent(
				evt.getSource());
		this.collectionModificationSupport
				.fireCollectionModificationEvent(simpleEvent);
	}

	@Override
	public Collection<E> getCollection() {
		if (predicate == null && comparator == null) {
			return colln;
		}
		ArrayList<E> l = new ArrayList<E>();
		if (predicate == null) {
			l = new ArrayList<>(colln);
		} else {
			for (E e : colln) {
				if (predicate.test(e)) {
					l.add(e);
				}
			}
		}
		if (comparator != null) {
			l.sort(comparator);
		}
		return l;
	}

	@Override
	public Class<? extends E> getCollectionMemberClass() {
		return this.baseClass;
	}

	@Override
	public int getCollectionSize() {
		return getCollection().size();
	}

	public Comparator<E> getComparator() {
		return this.comparator;
	}

	public Predicate<E> getPredicate() {
		return this.predicate;
	}

	@Override
	public void onDetach() {
		TransformManager.get().removeCollectionModificationListener(this);
	}

	@Override
	public void removeCollectionModificationListener(
			CollectionModificationListener listener) {
		this.collectionModificationSupport
				.removeCollectionModificationListener(listener);
	}

	public void setComparator(Comparator<E> comparator) {
		this.comparator = comparator;
	}

	public void setPredicate(Predicate<E> predicate) {
		this.predicate = predicate;
		collectionModification(new CollectionModificationEvent(this));
	}
}

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
package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Signals to listeners (collection nodes) that they should recalc their
 * collections
 * 
 * @author nick@alcina.cc
 * 
 */
public class CollectionModification {
	public interface CollectionModificationListener {
		public void collectionModification(CollectionModificationEvent evt);
	}

	public static class CollectionModificationEvent<T> {
		private Object source;

		private Class<? extends T> collectionClass;

		private Collection<T> collection;

		private boolean fromPropertyChange;

		public boolean isFromPropertyChange() {
			return this.fromPropertyChange;
		}

		public Collection<T> getCollection() {
			return this.collection;
		}

		public Class getCollectionClass() {
			return this.collectionClass;
		}

		public CollectionModificationEvent(Object source) {
			this.source = source;
		}

		public CollectionModificationEvent(Object source,
				Class<? extends T> collectionClass, Collection<T> collection) {
			this(source, collectionClass, collection, false);
		}

		public CollectionModificationEvent(Object source,
				Class<? extends T> collectionClass, Collection<T> collection,
				boolean fromPropertyChange) {
			this.collectionClass = collectionClass;
			this.source = source;
			this.collection = collection;
			this.fromPropertyChange = fromPropertyChange;
		}

		public Object getSource() {
			return this.source;
		}
	}

	public interface CollectionModificationSource {
		public void addCollectionModificationListener(
				CollectionModificationListener listener);

		public void removeCollectionModificationListener(
				CollectionModificationListener listener);
	}

	public static class CollectionModificationSupport implements
			CollectionModificationSource {
		private static class SupportEvent {
			private CollectionModificationEvent event;

			private CollectionModificationSupport support;

			public SupportEvent(CollectionModificationEvent event,
					CollectionModificationSupport support) {
				this.event = event;
				this.support = support;
			}
		}

		private List<CollectionModificationListener> listenerList = new ArrayList<CollectionModificationListener>();;

		private List<Tuple> classListenerList = new ArrayList<Tuple>();

		private static List<SupportEvent> queuedEvents = null;

		private static int queueDepth = 0;

		public static void queue(boolean push) {
			queueDepth += push ? 1 : -1;
			queueDepth = Math.max(queueDepth, 0);
			if (queueDepth == 0 && queuedEvents != null) {
				List<SupportEvent> queueCopy = queuedEvents;
				queuedEvents = null;
				for (SupportEvent supportEvent : queueCopy) {
					supportEvent.support
							.fireCollectionModificationEvent(supportEvent.event);
				}
			}
			if (queueDepth != 0 && queuedEvents == null) {
				queuedEvents = new ArrayList<SupportEvent>();
			}
		}

		public void addCollectionModificationListener(
				CollectionModificationListener listener) {
			listenerList.add(listener);
		}

		public void addCollectionModificationListener(
				CollectionModificationListener listener, Class listenedClass) {
			addCollectionModificationListener(listener, listenedClass, false);
		}

		public void addCollectionModificationListener(
				CollectionModificationListener listener, Class listenedClass,
				boolean filteringListener) {
			Tuple tuple = new Tuple();
			tuple.listenedClass = listenedClass;
			tuple.listener = listener;
			tuple.filteringListener = filteringListener;
			classListenerList.add(tuple);
		}

		public void removeCollectionModificationListener(
				CollectionModificationListener listener) {
			listenerList.remove(listener);
			Iterator<Tuple> itr = classListenerList.iterator();
			while (itr.hasNext()) {
				Tuple t = itr.next();
				if (t.listener == listener) {
					itr.remove();
				}
			}
		}

		public void fireCollectionModificationEvent(
				CollectionModificationEvent event) {
			if (listenerList.isEmpty() && classListenerList.isEmpty()) {
				return;
			}
			if (queuedEvents != null) {
				queuedEvents.add(new SupportEvent(event, this));
				return;
			}
			if (!event.isFromPropertyChange()) {
				for (CollectionModificationListener listener : listenerList) {
					listener.collectionModification(event);
				}
			}
			for (Tuple t : classListenerList) {
				if ((event.getCollectionClass() == Object.class || t.listenedClass == event
						.getCollectionClass())
						&& (!event.isFromPropertyChange() || t.filteringListener)) {
					t.listener.collectionModification(event);
				}
			}
		}

		private class Tuple {
			Class listenedClass;

			CollectionModificationListener listener;

			boolean filteringListener;
		}
	}
}

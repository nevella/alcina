package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public class FastIdLookupDev implements FastIdLookup {
	@SuppressWarnings("unused")
	private FastIdInfo info;

	private Map<Long, HasIdAndLocalId> idLookup = new LinkedHashMap<Long, HasIdAndLocalId>();

	private Map<Long, HasIdAndLocalId> localIdLookup = new LinkedHashMap<Long, HasIdAndLocalId>();

	private Class clazz;

	private FastIdLookupDevValues values;

	public FastIdLookupDev(Class clazz, FastIdInfo info) {
		this.clazz = clazz;
		this.info = info;
		this.values = new FastIdLookupDevValues();
	}

	class FastIdLookupDevValues implements Collection<HasIdAndLocalId> {
		@Override
		public boolean add(HasIdAndLocalId o) {
			HasIdAndLocalId hili = (HasIdAndLocalId) o;
			boolean contains = contains(o);
			put(hili, hili.getLocalId() == 0);
			return !contains;
		}

		@Override
		public boolean isEmpty() {
			return size() == 0;
		}

		@Override
		public Iterator<HasIdAndLocalId> iterator() {
			return new FastIdLookupDevItr();
		}

		@Override
		public boolean addAll(Collection<? extends HasIdAndLocalId> c) {
			return false;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof HasIdAndLocalId) {
				HasIdAndLocalId hili = (HasIdAndLocalId) o;
				if (hili.getLocalId() == 0) {
					return get(hili.getId(), false) != null;
				} else {
					return get(hili.getLocalId(), true) != null;
				}
			}
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return new ArrayList(this).containsAll(c);
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			return localIdLookup.size() + idLookup.size();
		}

		@Override
		public Object[] toArray() {
			ArrayList list = new ArrayList(size());
			for (HasIdAndLocalId hili : this) {
				list.add(hili);
			}
			return list.toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			ArrayList list = new ArrayList(size());
			for (HasIdAndLocalId hili : this) {
				list.add(hili);
			}
			return (T[]) list.toArray(a);
		}

		class FastIdLookupDevItr implements Iterator<HasIdAndLocalId> {
			int arrayCtr = -1;

			Iterator<HasIdAndLocalId> collectionIterator;

			HasIdAndLocalId poppedNextObject;

			boolean atEnd = false;

			boolean poppedNext = false;

			public FastIdLookupDevItr() {
			}

			@Override
			public boolean hasNext() {
				maybePopNext();
				return !atEnd;
			}

			@Override
			public HasIdAndLocalId next() {
				if (atEnd && !poppedNext) {
					throw new NoSuchElementException();
				}
				maybePopNext();
				poppedNext = false;
				return (HasIdAndLocalId) poppedNextObject;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			private void maybePopNext() {
				if (atEnd) {
					return;
				}
				if (!poppedNext) {
					poppedNext = true;
					atEnd = popNext();
					return;
				}
			}

			/*
			 * true == at end of iterator
			 */
			private boolean popNext() {
				poppedNextObject = null;
				if (arrayCtr == -1 || !collectionIterator.hasNext()) {
					arrayCtr++;
					switch (arrayCtr) {
					case 0:
						collectionIterator = idLookup.values().iterator();
						break;
					case 1:
						collectionIterator = localIdLookup.values().iterator();
						break;
					case 2:
						return true;
					}
					return popNext();
				}
				poppedNextObject = collectionIterator.next();
				return false;
			}
		}
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lkp - %s - [%s,%s]",
				CommonUtils.classSimpleName(clazz), idLookup.size(),
				localIdLookup.size());
	}

	@Override
	public HasIdAndLocalId get(long id, boolean local) {
		checkId(id);
		if (local) {
			return localIdLookup.get(id);
		} else {
			return idLookup.get(id);
		}
	}

	@Override
	public void put(HasIdAndLocalId hili, boolean local) {
		long idi = getApplicableId(hili, local);
		if (local) {
			localIdLookup.put(idi, hili);
		} else {
			idLookup.put(idi, hili);
		}
	}

	@Override
	public void remove(long id, boolean local) {
		checkId(id);
		if (local) {
			localIdLookup.remove(id);
		} else {
			idLookup.remove(id);
		}
	}

	long getApplicableId(HasIdAndLocalId hili, boolean local) {
		long id = local ? hili.getLocalId() : hili.getId();
		checkId(id);
		return id;
	}

	public void checkId(long id) {
		if (id > LongWrapperHash.MAX) {
			throw new RuntimeException("losing higher bits from long");
		}
	}

	@Override
	public Collection<HasIdAndLocalId> values() {
		return values;
	}
}
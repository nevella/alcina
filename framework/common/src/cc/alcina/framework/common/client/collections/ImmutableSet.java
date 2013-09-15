package cc.alcina.framework.common.client.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ImmutableSet<E> implements Set<E> {
	private Set<E> delegate;

	public ImmutableSet(Set<E> delegate) {
		this.delegate = delegate;
	}

	public int size() {
		return this.delegate.size();
	}

	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	public boolean contains(Object o) {
		return this.delegate.contains(o);
	}

	public Iterator<E> iterator() {
		return this.delegate.iterator();
	}

	public Object[] toArray() {
		return this.delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return this.delegate.toArray(a);
	}

	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(Collection<?> c) {
		return this.delegate.containsAll(c);
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean equals(Object o) {
		return this.delegate.equals(o);
	}

	public int hashCode() {
		return this.delegate.hashCode();
	}
}

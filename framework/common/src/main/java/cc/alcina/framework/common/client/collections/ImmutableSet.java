package cc.alcina.framework.common.client.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ImmutableSet<E> implements Set<E> {
	private Set<E> delegate;

	public ImmutableSet(Set<E> delegate) {
		this.delegate = delegate;
	}

	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Object o) {
		return this.delegate.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return this.delegate.containsAll(c);
	}

	public boolean equals(Object o) {
		return this.delegate.equals(o);
	}

	public int hashCode() {
		return this.delegate.hashCode();
	}

	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	public Iterator<E> iterator() {
		return this.delegate.iterator();
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public int size() {
		return this.delegate.size();
	}

	public Object[] toArray() {
		return this.delegate.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return this.delegate.toArray(a);
	}
}

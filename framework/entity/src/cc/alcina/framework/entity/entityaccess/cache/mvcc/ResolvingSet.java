package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.Entity;

/**
 * 
 * @author nick@alcina.cc
 *
 *         since we instrument all non-private methods for all domain objects,
 *         probably won't use
 *
 * @param <E>
 */
@Deprecated
public class ResolvingSet<E extends Entity> implements Set<E> {
    private Set<E> delegate;

    ResolvingSet(Set<E> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean add(E e) {
        return this.delegate.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return this.delegate.addAll(c);
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public boolean contains(Object o) {
        return this.delegate.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.delegate.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        return this.delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return new ResolvingSetIterator(this.delegate.iterator());
    }

    @Override
    public boolean remove(Object o) {
        return this.delegate.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.delegate.retainAll(c);
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public Object[] toArray() {
        return this.delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this.delegate.toArray(a);
    }

    class ResolvingSetIterator implements Iterator<E> {
        private Iterator<E> delegateIterator;

        public ResolvingSetIterator(Iterator<E> delegateIterator) {
            this.delegateIterator = delegateIterator;
        }

        @Override
        public boolean hasNext() {
            return this.delegateIterator.hasNext();
        }

        @Override
        public E next() {
            return Transactions.resolve(this.delegateIterator.next(), false);
        }
    }
}

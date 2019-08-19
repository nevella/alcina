package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class MultiIterator<E> implements Iterator<E> {
    private Iterator<E>[] iterators;

    private int currentIteratorIndex = 0;

    private boolean allowRemove;

    public MultiIterator(boolean allowRemove, Iterator<E>... iterators) {
        this.allowRemove = allowRemove;
        this.iterators = iterators;
    }

    public Iterator<E> getCurrentIterator() {
        while (currentIteratorIndex < iterators.length) {
            if (iterators[currentIteratorIndex].hasNext()) {
                return iterators[currentIteratorIndex];
            }
            currentIteratorIndex++;
        }
        return null;
    }

    public int getCurrentIteratorIndex() {
        return currentIteratorIndex;
    }

    @Override
    public boolean hasNext() {
        return getCurrentIterator() != null;
    }

    @Override
    public E next() {
        Iterator<E> itr = getCurrentIterator();
        if (itr == null) {
            throw new NoSuchElementException();
        }
        return itr.next();
    }

    @Override
    public void remove() {
        if (allowRemove) {
            Iterator<E> itr = iterators[currentIteratorIndex];
            if (itr == null) {
                throw new NoSuchElementException();
            }
            itr.remove();
            return;
        }
        throw new IllegalArgumentException("Remove not permitted");
    }
}

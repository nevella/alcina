package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class MultiIterator<E> implements Iterator<E> {
	private Iterator<E>[] iterators;

	private int idx = 0;

	private boolean allowRemove;

	public MultiIterator(boolean allowRemove, Iterator<E>... iterators) {
		this.allowRemove = allowRemove;
		this.iterators = iterators;
	}

	@Override
	public boolean hasNext() {
		return ensureCurrentIterator() != null;
	}

	private Iterator<E> ensureCurrentIterator() {
		while (idx < iterators.length) {
			if (iterators[idx].hasNext()) {
				return iterators[idx];
			}
			idx++;
		}
		return null;
	}
	@Override
	public E next() {
		Iterator<E> itr = ensureCurrentIterator();
		if (itr == null) {
			throw new NoSuchElementException();
		}
		return itr.next();
	}

	@Override
	public void remove() {
		if (allowRemove) {
			Iterator<E> itr = iterators[idx];
			if (itr == null) {
				throw new NoSuchElementException();
			}
			itr.remove();
			return;
		}
		throw new IllegalArgumentException("Remove not permitted");
	}
}

package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class FilteringIterator<E> implements Iterator<E> {
	private Iterator<E> source;

	private E next;

	private boolean finished;

	private boolean peeked;

	private Predicate<E> filter;

	public FilteringIterator(Iterator<E> source, Predicate<E> filter) {
		this.source = source;
		this.filter = filter;
	}

	@Override
	public boolean hasNext() {
		if (!finished) {
			peek();
		}
		return !finished;
	}

	@Override
	public E next() {
		if (finished) {
			throw new NoSuchElementException();
		}
		peek();
		if (finished) {
			throw new NoSuchElementException();
		}
		peeked = false;
		return next;
	}

	private void peek() {
		if (peeked) {
			return;
		}
		peeked = true;
		// not needed, but sanitary
		next = null;
		while (source.hasNext()) {
			E sourceNext = source.next();
			if (filter.test(sourceNext)) {
				next = sourceNext;
				return;
			}
		}
		finished = true;
	}
}

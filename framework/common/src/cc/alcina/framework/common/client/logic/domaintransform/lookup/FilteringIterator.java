package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class FilteringIterator<E> implements Iterator<E> {
	public static <E> FilteringIterator<E> wrap(Iterator<E> source) {
		return source instanceof FilteringIterator
				? (FilteringIterator<E>) source
				: new FilteringIterator<>(source, e -> true);
	}

	private Iterator<E> source;

	protected E next;

	protected boolean finished;

	protected boolean peeked;

	private Predicate<E> filter;

	public FilteringIterator(Iterator<E> source, Predicate<E> filter) {
		this.source = source;
		this.filter = filter;
	}

	protected FilteringIterator() {
		// for multi-iterator
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
		resetPeeked();
		if (next == null) {
			int debug = 3;
		}
		return next;
	}

	public E peek() {
		if (finished) {
			throw new NoSuchElementException();
		}
		if (peeked) {
			return next;
		}
		peeked = true;
		// not needed, but sanitary
		next = null;
		return peekNext();
	}

	protected E peekNext() {
		while (source.hasNext()) {
			E sourceNext = source.next();
			if (filter.test(sourceNext)) {
				next = sourceNext;
				return next;
			}
		}
		finished = true;
		return null;
	}

	protected void resetPeeked() {
		peeked = false;
	}
}

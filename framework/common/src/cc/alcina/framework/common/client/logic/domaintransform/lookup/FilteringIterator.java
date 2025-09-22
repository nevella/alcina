package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;
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

	public boolean isFinished() {
		return finished;
	}

	protected boolean peeked;

	private Predicate<E> filter;

	protected FilteringIterator() {
		// for multi-iterator
	}

	public FilteringIterator(Iterator<E> source, Predicate<E> filter) {
		this.source = source;
		this.filter = filter;
	}

	public Iterator<E> getSource() {
		return this.source;
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
		return next;
	}

	/*
	 * Guard calls to this method with a check to isFinished()
	 */
	public E peek() {
		if (finished) {
			throw new NoSuchElementException();
		}
		if (peeked) {
			return next;
		}
		peeked = true;
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

	/**
	 * 
	 * @param <T>
	 * @param collection
	 * @return a filtering iterator across the collection that returns all
	 *         elements (but provides a 'current' aka peek)
	 */
	public static <T> FilteringIterator<T> wrap(Collection<T> collection) {
		return new FilteringIterator<>(collection.iterator(), e -> true);
	}
}

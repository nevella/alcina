package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class MultiIterator<E> extends FilteringIterator<E> {
	private List<FilteringIterator<E>> iterators;

	private int currentIteratorIndex = 0;

	private boolean allowRemove;

	private Comparator<E> comparator;

	public MultiIterator(boolean allowRemove, Comparator<E> comparator,
			Iterator<E>... iterators) {
		this.allowRemove = allowRemove;
		this.comparator = comparator;
		this.iterators = Arrays.asList(iterators).stream()
				.map(FilteringIterator::wrap).collect(Collectors.toList());
	}

	public int getCurrentIteratorIndex() {
		return currentIteratorIndex;
	}

	@Override
	public void remove() {
		if (allowRemove) {
			Iterator<E> itr = iterators.get(currentIteratorIndex);
			if (itr == null) {
				throw new NoSuchElementException();
			}
			itr.remove();
			return;
		}
		throw new IllegalArgumentException("Remove not permitted");
	}

	private E peekNonSorted() {
		peeked = true;
		while (currentIteratorIndex < iterators.size()) {
			if (iterators.get(currentIteratorIndex).hasNext()) {
				FilteringIterator<E> filteringIterator = iterators
						.get(currentIteratorIndex);
				next = filteringIterator.next();
				return next;
			}
			currentIteratorIndex++;
		}
		finished = true;
		return null;
	}

	private E peekSorted() {
		E max = null;
		peeked = true;
		currentIteratorIndex = -1;
		for (int idx = 0; idx < iterators.size(); idx++) {
			FilteringIterator<E> iterator = iterators.get(idx);
			if (!iterator.hasNext()) {
				continue;
			}
			E e = iterator.peek();
			if (max == null || comparator.compare(max, e) < 0) {
				max = e;
				currentIteratorIndex = idx;
			}
		}
		finished = max == null;
		return max;
	}

	@Override
	protected E peekNext() {
		if (comparator == null) {
			return peekNonSorted();
		} else {
			return peekSorted();
		}
	}

	@Override
	protected void resetPeeked() {
		super.resetPeeked();
		if (currentIteratorIndex != -1) {
			iterators.get(currentIteratorIndex).peeked = false;
		}
	}
}

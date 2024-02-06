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

	public FilteringIterator getCurrentIterator() {
		return iterators.get(currentIteratorIndex);
	}

	public int getCurrentIteratorIndex() {
		return currentIteratorIndex;
	}

	protected void onBeforeIteratorIndexChange(int currentIteratorIndex) {
		// For subclasses
	}

	@Override
	protected E peekNext() {
		if (comparator == null) {
			peekNonSorted();
		} else {
			peekSorted();
		}
		return next;
	}

	private void peekNonSorted() {
		peeked = true;
		while (currentIteratorIndex < iterators.size()) {
			if (iterators.get(currentIteratorIndex).hasNext()) {
				FilteringIterator<E> filteringIterator = getCurrentIterator();
				next = filteringIterator.next();
				return;
			}
			onBeforeIteratorIndexChange(currentIteratorIndex);
			currentIteratorIndex++;
		}
		finished = true;
	}

	private void peekSorted() {
		boolean minPopulated = false;
		E min = null;
		peeked = true;
		currentIteratorIndex = -1;
		for (int iteratorIdx = 0; iteratorIdx < iterators
				.size(); iteratorIdx++) {
			FilteringIterator<E> iterator = iterators.get(iteratorIdx);
			if (!iterator.hasNext()) {
				continue;
			}
			E e = iterator.peek();
			// requires null safe comparator
			if (!minPopulated || comparator.compare(e, min) < 0) {
				min = e;
				currentIteratorIndex = iteratorIdx;
			}
		}
		finished = currentIteratorIndex == -1;
		next = min;
	}

	@Override
	public void remove() {
		if (allowRemove) {
			Iterator<E> itr = getCurrentIterator();
			if (itr == null) {
				throw new NoSuchElementException();
			}
			itr.remove();
			return;
		}
		throw new IllegalArgumentException("Remove not permitted");
	}

	@Override
	protected void resetPeeked() {
		super.resetPeeked();
		if (currentIteratorIndex != -1) {
			getCurrentIterator().peeked = false;
		}
	}
}

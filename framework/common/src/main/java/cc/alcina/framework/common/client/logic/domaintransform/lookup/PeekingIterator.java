package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class PeekingIterator<E> implements Iterator<E> {
	protected E next;

	protected boolean finished;

	protected boolean peeked;

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

	void peek() {
		if (finished) {
			throw new NoSuchElementException();
		}
		if (peeked) {
			return;
		}
		peeked = true;
		next = peekNext();
	}

	/**
	 * Note that this method must also set finished to true if the element
	 * source is finished
	 * 
	 * @return the next value (or null if finished)
	 */
	protected abstract E peekNext();

	protected void resetPeeked() {
		peeked = false;
	}
}

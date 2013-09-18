package cc.alcina.framework.common.client.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorWithCurrent<T> {
	private Iterator<T> itr;

	public IteratorWithCurrent(Iterator<T> itr) {
		this.itr = itr;
		if (itr.hasNext()) {
			moveNext();
		}
	}

	T current;

	public T current() {
		return current;
	}

	public void moveNext() {
		if (!itr.hasNext()) {
			current = null;
		} else {
			current = itr.next();
		}
	}
}
package cc.alcina.framework.common.client.collections;

import java.util.Iterator;

public class IteratorWithCurrent<T> {
	private Iterator<T> itr;

	T current;

	public IteratorWithCurrent(Iterator<T> itr) {
		this.itr = itr;
		if (itr.hasNext()) {
			moveNext();
		}
	}

	public T current() {
		return current;
	}

	public Iterator<T> getItr() {
		return this.itr;
	}

	public void moveNext() {
		if (!itr.hasNext()) {
			current = null;
		} else {
			current = itr.next();
		}
	}
}
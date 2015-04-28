package java.util.stream;

import java.util.Collection;
import java.util.Iterator;

public class CollectionStream<E> implements Stream<E> {
	private Collection<E> collection;

	public CollectionStream(Collection<E> collection) {
		this.collection = collection;
	}

	@Override
	public Iterator<E> iterator() {
		return collection.iterator();
	}
}
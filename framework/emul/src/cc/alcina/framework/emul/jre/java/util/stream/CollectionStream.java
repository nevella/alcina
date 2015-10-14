package java.util.stream;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class CollectionStream<E> implements Stream<E> {
	Collection<E> collection;

	public CollectionStream(Collection<E> collection) {
		this.collection = collection;
	}

	@Override
	public Iterator<E> iterator() {
		return collection.iterator();
	}

	List<E> asList() {
		return new ArrayList<>(collection);
	}
}
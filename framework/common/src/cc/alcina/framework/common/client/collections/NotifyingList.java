package cc.alcina.framework.common.client.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.RunnableCallable;
import cc.alcina.framework.common.client.util.Topic;

/**
 * <p>
 * NotifyingList wraps a delegate list (such as an ArrayList) and emits events
 * before and after mutations
 * 
 */
public class NotifyingList<T> implements List<T> {
	public Topic<Notification> topicNotifications = Topic.create();

	List<T> delegate;

	public NotifyingList(List<T> delegate) {
		this.delegate = delegate;
	}

	public void add(int index, T element) {
		withMutating(() -> delegate.add(index, element), element, true);
	}

	public boolean add(T e) {
		return withMutating(() -> delegate.add(e), e, true, null);
	}

	public boolean addAll(Collection<? extends T> c) {
		if (c.isEmpty()) {
			return false;
		}
		c.forEach(this::add);
		return true;
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public void forEach(Consumer<? super T> action) {
		delegate.forEach(action);
	}

	public T get(int index) {
		return delegate.get(index);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public int indexOf(Object o) {
		return delegate.indexOf(o);
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public Iterator<T> iterator() {
		return delegate.iterator();
	}

	public int lastIndexOf(Object o) {
		return delegate.lastIndexOf(o);
	}

	public ListIterator<T> listIterator() {
		return delegate.listIterator();
	}

	public ListIterator<T> listIterator(int index) {
		return delegate.listIterator(index);
	}

	public Stream<T> parallelStream() {
		return delegate.parallelStream();
	}

	public T remove(int index) {
		T element = get(index);
		return withMutating(() -> delegate.remove(index), element, false,
				v -> (boolean) v);
	}

	public boolean remove(Object o) {
		return withMutating(() -> delegate.remove(o), (T) o, false,
				v -> (boolean) v);
	}

	public boolean removeAll(Collection<?> c) {
		boolean delta = false;
		for (Object o : c) {
			delta |= remove(o);
		}
		return delta;
	}

	public boolean removeIf(Predicate<? super T> filter) {
		throw new UnsupportedOperationException();
	}

	public void replaceAll(UnaryOperator<T> operator) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public T set(int index, T element) {
		return withMutating(() -> delegate.set(index, element), element, true,
				null);
	}

	public int size() {
		return delegate.size();
	}

	public void sort(Comparator<? super T> c) {
		throw new UnsupportedOperationException();
	}

	public Spliterator<T> spliterator() {
		return delegate.spliterator();
	}

	public Stream<T> stream() {
		return delegate.stream();
	}

	public List<T> subList(int fromIndex, int toIndex) {
		return delegate.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		return delegate.toArray();
	}

	public <E> E[] toArray(E[] a) {
		return delegate.toArray(a);
	}

	<V> V withMutating(Callable<V> callable, T delta, boolean add,
			Predicate<V> hadMutationPredicate) {
		try {
			topicNotifications.publish(new Notification(false));
			V v = callable.call();
			if (hadMutationPredicate == null || hadMutationPredicate.test(v)) {
				topicNotifications.publish(new Notification(true, delta, add));
			}
			return v;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	void withMutating(Runnable runnable, T delta, boolean add) {
		withMutating(new RunnableCallable(runnable), delta, add, null);
	}

	public class Notification {
		public final boolean postMutation;

		public final T delta;

		public final boolean add;

		public Notification(boolean postMutation) {
			this(postMutation, null, false);
		}

		public Notification(boolean postMutation, T delta, boolean add) {
			this.postMutation = postMutation;
			this.delta = delta;
			this.add = add;
		}

		public List<T> snapshot() {
			return new ArrayList<>(delegate);
		}
	}
}

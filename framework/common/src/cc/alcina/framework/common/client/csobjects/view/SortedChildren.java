package cc.alcina.framework.common.client.csobjects.view;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SortedChildren<T> implements List<TreePath<T>> {
	private TreeSet<TreePath<T>> children = new TreeSet<>(Cmp.INSTANCE);

	@Override
	public void add(int index, TreePath<T> element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(TreePath<T> e) {
		return this.children.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends TreePath<T>> c) {
		return this.children.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends TreePath<T>> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		this.children.clear();
	}

	public Comparator<? super TreePath<T>> comparator() {
		return this.children.comparator();
	}

	@Override
	public boolean contains(Object o) {
		return this.children.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.children.containsAll(c);
	}

	@Override
	public boolean equals(Object o) {
		return this.children.equals(o);
	}

	@Override
	public void forEach(Consumer<? super TreePath<T>> action) {
		this.children.forEach(action);
	}

	@Override
	public TreePath<T> get(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		return this.children.hashCode();
	}

	@Override
	public int indexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return this.children.isEmpty();
	}

	@Override
	public Iterator<TreePath<T>> iterator() {
		return this.children.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<TreePath<T>> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<TreePath<T>> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Stream<TreePath<T>> parallelStream() {
		return this.children.parallelStream();
	}

	@Override
	public TreePath<T> remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		return this.children.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return this.children.removeAll(c);
	}

	@Override
	public boolean removeIf(Predicate<? super TreePath<T>> filter) {
		return this.children.removeIf(filter);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.children.retainAll(c);
	}

	@Override
	public TreePath<T> set(int index, TreePath<T> element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return this.children.size();
	}

	@Override
	public Spliterator<TreePath<T>> spliterator() {
		return this.children.spliterator();
	}

	@Override
	public Stream<TreePath<T>> stream() {
		return this.children.stream();
	}

	@Override
	public List<TreePath<T>> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	public TreePath<T> successor(TreePath<T> treePath) {
		return children.higher(treePath);
	}

	@Override
	public Object[] toArray() {
		return this.children.toArray();
	}

	@Override
	public <V> V[] toArray(V[] a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return this.children.toString();
	}

	private static class Cmp implements Comparator<TreePath> {
		private static final Cmp INSTANCE = new Cmp();

		@Override
		public int compare(TreePath o1, TreePath o2) {
			return o1.segmentComparable.compareTo(o2.segmentComparable);
		}
	}
}
package cc.alcina.framework.common.client.csobjects.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 * This could be implemented with each treepath having a Map<String,Child> - but this way seems a lot slimmer
 */
public class TreePath<T> extends Model
		implements HasFilteredSelfAndDescendantCount {
	public static TreePath absolutePath(String path) {
		Preconditions.checkArgument(path.length() > 0);
		TreePath<?> root = root(path.split("\\.")[0]);
		return root.ensurePath(path);
	}

	public static String parentPath(String treePath) {
		int lastIndex = treePath.lastIndexOf(".");
		return lastIndex == -1 ? null : treePath.substring(0, lastIndex);
	}

	public static <T> TreePath<T> root(Object rootSegment) {
		TreePath root = new TreePath();
		root.withSegment(rootSegment);
		root.paths = new Paths(root);
		return root;
	}

	private transient Paths paths;

	private TreePath<T> parent;

	/*
	 * Note that children are unordered - it's the order of the children in the
	 * corresponding NodeModel that matters
	 */
	private transient List<TreePath<T>> children;

	private transient String cached;

	private transient T value;

	transient Comparable segmentComparable;

	private String segment = "";

	private transient int selfAndDescendantCount = 1;

	private transient boolean onlyLeafChildren;

	// Should only be used by serialization
	public TreePath() {
	}

	public TreePath<T> addChild(Object segmentObject) {
		ensureChildren();
		return addChild(segmentObject, children.size());
	}

	public TreePath<T> addChild(Object segmentObject, int index) {
		ensureChildren();
		TreePath<T> child = new TreePath();
		String asSegment = asSegment(segmentObject);
		child.setSegment(asSegment);
		child.segmentComparable = paths.segmentComparable;
		addChildPath(index, child);
		recalculateCount();
		return child;
	}

	public void clearNonRoot() {
		paths.clearNonRoot();
	}

	public int depth() {
		int depth = 0;
		TreePath cursor = parent;
		while (cursor != null) {
			depth++;
			cursor = cursor.parent;
		}
		return depth;
	}

	public void dump(int depth, int maxDepth, Predicate<TreePath> treePredicate,
			DumpStyle style) {
		FormatBuilder fb = new FormatBuilder();
		fb.indent(depth * 2);
		switch (style) {
		case NON_LEAF_SIZES: {
			if (hasChildren()) {
				fb.format("%s [%s,%s]", segment,
						children.stream().filter(treePredicate::test).count(),
						subtreeSize(treePredicate));
			} else {
				return;
			}
		}
		case STRUCTURE: {
			fb.append(toString());
			break;
		}
		case STRUCTURE_AND_VALUE: {
			fb.append("");
			fb.appendPadRight(40, toString());
			fb.append(getValue());
			break;
		}
		default:
			throw new UnsupportedOperationException();
		}
		Ax.out(fb.toString());
		if (depth < maxDepth) {
			getChildren().forEach(
					n -> n.dump(depth + 1, maxDepth, treePredicate, style));
		}
	}

	public TreePath<T> ensureChild(Object segment,
			Comparable segmentComparable) {
		paths.segmentComparable = segmentComparable;
		return ensureChildPath(segment);
	}

	/*
	 * Prefer getPath() - unless the node must be created if absent
	 */
	public TreePath<T> ensurePath(String stringPath) {
		return paths.ensurePath(stringPath);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TreePath && toString().equals(obj.toString());
	}

	public <E extends Entity> E find(Class<E> clazz) {
		return Domain.find(clazz, segmentAsLong());
	}

	public Optional<TreePath<T>> getChildPath(Object segment) {
		return getPath(childPath(segment));
	}

	@AlcinaTransient
	@XmlTransient
	@JsonIgnore
	public List<TreePath<T>> getChildren() {
		ensureChildren();
		return this.children;
	}

	public TreePath<T> getParent() {
		return this.parent;
	}

	public Optional<TreePath<T>> getPath(String stringPath) {
		return (Optional<TreePath<T>>) (Optional<?>) paths.getPath(stringPath);
	}

	public String getSegment() {
		return this.segment;
	}

	public int getSelfAndDescendantCount() {
		return this.selfAndDescendantCount;
	}

	@AlcinaTransient
	@XmlTransient
	@JsonIgnore
	public T getValue() {
		return this.value;
	}

	public boolean hasAncestorMatching(Predicate<TreePath<T>> predicate) {
		return parent != null && parent.hasSelfOrAncestorMatching(predicate);
	}

	public boolean hasChildPath(Object segment) {
		return hasPath(toString() + "." + asSegment(segment));
	}

	public boolean hasChildren() {
		return children != null && children.size() > 0;
	}

	public boolean hasChildrenLoaded() {
		return children != null;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public boolean hasPath(String stringPath) {
		return paths.hasPath(stringPath);
	}

	public boolean hasSelfOrAncestorMatching(Predicate<TreePath<T>> predicate) {
		TreePath<T> cursor = this;
		while (cursor != null) {
			if (predicate.test(cursor)) {
				return true;
			}
			cursor = cursor.parent;
		}
		return false;
	}

	public <CT> CT provideContainingTree() {
		return (CT) paths.containingTree;
	}

	public boolean provideIsEmpty() {
		return toString().isEmpty();
	}

	public boolean provideIsLeaf() {
		return !hasChildren();
	}

	@Override
	public int provideSelfAndDescendantCount(Object filter) {
		if (getValue() != null
				&& (getValue() instanceof HasFilteredSelfAndDescendantCount)) {
			HasFilteredSelfAndDescendantCount filtered = (HasFilteredSelfAndDescendantCount) getValue();
			int selfAndDescendantCount = filtered
					.provideSelfAndDescendantCount(filter);
			if (selfAndDescendantCount != -1) {
				return selfAndDescendantCount;
			}
		}
		return getSelfAndDescendantCount();
	}

	public String provideSuccessorPath() {
		if (getParent() == null) {
			return null;
		}
		List<TreePath<T>> children = getParent().ensureChildren();
		Preconditions.checkState(children instanceof SortedChildren);
		SortedChildren<T> sortedChildren = (SortedChildren<T>) children;
		TreePath<T> successor = sortedChildren.successor(this);
		return successor == null ? null : successor.toString();
	}

	public void putSortedChildren() {
		paths.childListCreator = () -> new SortedChildren();
	}

	public void putTree(Object containingTree) {
		paths.putTree(containingTree);
	}

	public void removeFromParent() {
		removeFromParent(true);
	}

	public TreePath<T> rootPath() {
		return paths.root;
	}

	public void setChildren(List<TreePath<T>> children) {
		this.children = children;
	}

	public void setParent(TreePath parent) {
		this.parent = parent;
	}

	public void setSegment(String segment) {
		this.segment = segment;
	}

	public void setSelfAndDescendantCount(int selfAndDescendantCount) {
		this.selfAndDescendantCount = selfAndDescendantCount;
	}

	public void setValue(T value) {
		T old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	@Override
	public String toString() {
		if (cached == null) {
			cached = parent == null || parent.toString().isEmpty() ? segment
					: parent.toString() + "." + segment;
		}
		return cached;
	}

	public void trace(boolean trace) {
		paths.trace = trace;
	}

	public Walker<T> walker() {
		return new Walker(this);
	}

	public TreePath withSegment(Object object) {
		segment = asSegment(object);
		return this;
	}

	private void addChildPath(int index, TreePath<T> child) {
		child.setParent(this);
		child.paths = paths;
		paths.put(child);
		if (index == children.size()) {
			children.add(child);
		} else {
			children.add(index, child);
		}
	}

	private String asSegment(Object object) {
		String segment = asSegment0(object);
		Preconditions.checkArgument(!segment.contains("."));
		return segment;
	}

	private String asSegment0(Object object) {
		if (object instanceof String) {
			return (String) object;
		}
		if (object instanceof Entity) {
			return String.valueOf(((Entity) object).getId());
		} else {
			return CommonUtils.friendlyConstant(object, "_").toLowerCase();
		}
	}

	private TreePath<T> ensureChildPath(Object segment) {
		return ensurePath(childPath(segment));
	}

	private List<TreePath<T>> ensureChildren() {
		if (children == null) {
			children = paths.createChildList();
		}
		return children;
	}

	private void removeFromParent(boolean recalc) {
		// concurrent dance
		if (children != null) {
			children.stream().collect(Collectors.toList())
					//
					.forEach(path -> path.removeFromParent(false));
		}
		paths.remove(toString());
		parent.children.remove(this);
		if (recalc) {
			parent.recalculateCount();
		}
		parent = null;
	}

	private long segmentAsLong() {
		return Long.parseLong(getSegment());
	}

	private int subtreeSize(Predicate<TreePath> treePredicate) {
		int size = 0;
		Stack<TreePath<?>> stack = new Stack<>();
		stack.add(this);
		while (stack.size() > 0) {
			TreePath<?> path = stack.pop();
			if (!treePredicate.test(path)) {
				continue;
			}
			size++;
			if (hasChildren()) {
				path.getChildren().forEach(stack::add);
			}
		}
		return size;
	}

	protected void recalculateCount() {
		/*
		 * inefficient (generally at worst n logn, unless we have a non-leaf
		 * layer with high cardinality)) - but saves a lot of gallumphing with
		 * phases
		 */
		selfAndDescendantCount = 1 + (onlyLeafChildren ? getChildren().size()
				: getChildren().stream().collect(Collectors
						.summingInt(TreePath::getSelfAndDescendantCount)));
		if (parent != null) {
			parent.onlyLeafChildren = false;
			parent.recalculateCount();
		}
	}

	String childPath(Object segment) {
		return toString() + "." + asSegment(segment);
	}

	public static class DepthSegmentComparator
			implements Comparator<TreePath<?>> {
		static final SegmentComparator INSTANCE = new SegmentComparator();

		private boolean reverseIfSameSegment;

		public DepthSegmentComparator(boolean reverseIfSameSegment) {
			this.reverseIfSameSegment = reverseIfSameSegment;
		}

		@Override
		public int compare(TreePath<?> o1, TreePath<?> o2) {
			if (o1.hasAncestorMatching(t -> t == o2)) {
				return 1;
			}
			if (o2.hasAncestorMatching(t -> t == o1)) {
				return -1;
			}
			TreePath<?> c1 = o1;
			TreePath<?> c2 = o2;
			while (c1.depth() != c2.depth()) {
				if (c1.depth() > c2.depth()) {
					c1 = c1.getParent();
				} else {
					c2 = c2.getParent();
				}
			}
			while (c1.getParent() != c2.getParent()) {
				c1 = c1.getParent();
				c2 = c2.getParent();
			}
			int segmentComparison = SegmentComparator.INSTANCE.compare(c1, c2);
			return segmentComparison * (reverseIfSameSegment ? -1 : 1);
		}
	}

	public enum DumpStyle {
		STRUCTURE, STRUCTURE_AND_VALUE, NON_LEAF_SIZES
	}

	@Reflected
	public static enum Operation {
		INSERT, CHANGE, REMOVE;

		public int transformApplicationOrder() {
			switch (this) {
			case REMOVE:
				return 0;
			case CHANGE:
				return 1;
			case INSERT:
				return 2;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	public static class SegmentComparator implements Comparator<TreePath> {
		static final SegmentComparator INSTANCE = new SegmentComparator();

		@Override
		public int compare(TreePath o1, TreePath o2) {
			Preconditions.checkArgument(o1.parent == o2.parent);
			// FIXME - dirndl 1.2 - the segmentcomparable (either side) may be
			// null if the path was populated pre-transform - e.g. via being the
			// start location of a place
			//
			// fix is to populate with the segment comparable wherever possible
			// - fancy fix is to reorder on that population
			return CommonUtils.compareWithNullMinusOne(o1.segmentComparable,
					o2.segmentComparable);
		}
	}

	public static class Walker<T> {
		TreePath<T> current;

		public Walker(TreePath<T> from) {
			current = from;
		}

		public T current() {
			return this.current.getValue();
		}

		public TreePath next() {
			boolean tryDepth = true;
			while (true) {
				if (tryDepth && current.getChildren().size() > 0) {
					current = current.getChildren().get(0);
					return current;
				} else {
					if (current.getParent() == null) {
						return null;
					} else {
						List<TreePath<T>> siblings = current.getParent()
								.getChildren();
						int idx = siblings.indexOf(current);
						if (idx < siblings.size() - 1) {
							current = siblings.get(idx + 1);
							return current;
						} else {
							tryDepth = false;
							current = current.getParent();
						}
					}
				}
			}
		}

		public TreePath previous() {
			while (true) {
				if (current.getParent() == null) {
					return null;
				} else {
					List<TreePath<T>> siblings = current.getParent()
							.getChildren();
					if (siblings instanceof SortedChildren) {
						current = ((SortedChildren) siblings).previous(current);
						return current;
					} else {
						int idx = siblings.indexOf(current);
						if (idx > 0) {
							current = siblings.get(idx - 1);
							while (current.getChildren().size() > 0) {
								current = Ax.last(current.getChildren());
							}
							return current;
						} else {
							current = current.getParent();
						}
					}
				}
			}
		}

		public Stream<T> stream() {
			return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
					new IteratorImpl(), Spliterator.ORDERED), false);
		}

		private class IteratorImpl implements Iterator<T> {
			boolean returnedCurrent = false;

			boolean finished = false;

			@Override
			public boolean hasNext() {
				checkCurrent();
				return !finished;
			}

			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				returnedCurrent = true;
				return current();
			}

			private void checkCurrent() {
				if (returnedCurrent) {
					finished = Walker.this.next() == null;
					returnedCurrent = false;
				}
			}
		}
	}

	static class Paths {
		Comparable segmentComparable;

		TreePath root;

		Map<String, TreePath> byString = new LinkedHashMap<>();

		Object containingTree;

		Supplier<List> childListCreator = () -> new ArrayList();

		boolean trace;

		Paths(TreePath root) {
			this.root = root;
			put(root);
		}

		private void checkTrace(String op, String path) {
			if (!GWT.isClient()) {
				if (trace) {
					Ax.out("treePath:%s:%s", op, path);
				}
			}
		}

		void clearNonRoot() {
			byString.entrySet().removeIf(e -> e.getValue() != root);
		}

		List createChildList() {
			return childListCreator.get();
		}

		TreePath ensurePath(String stringPath) {
			String cursor = stringPath;
			TreePath treePath = null;
			int idx = -1;
			while (true) {
				treePath = byString.get(cursor);
				if (treePath != null) {
					break;
				}
				idx = cursor.lastIndexOf(".");
				cursor = cursor.substring(0, idx);
			}
			while (true) {
				if (cursor.length() == stringPath.length()) {
					segmentComparable = null;
					return treePath;
				}
				int segmentStart = cursor.length() + 1;
				idx = stringPath.indexOf(".", segmentStart);
				cursor = idx == -1 ? stringPath : stringPath.substring(0, idx);
				String segment = cursor.substring(segmentStart,
						cursor.length());
				checkTrace("add", stringPath);
				if (segmentComparable != null) {
					// require this to be the last segment
					Preconditions.checkArgument(idx == -1);
				}
				treePath = treePath.addChild(segment);
			}
		}

		Optional<TreePath> getPath(String path) {
			return Optional.ofNullable(byString.get(path));
		}

		boolean hasPath(String stringPath) {
			return byString.containsKey(stringPath);
		}

		void put(TreePath path) {
			String stringPath = path.toString();
			if (!byString.containsKey(stringPath)) {
				checkTrace("add", stringPath);
			}
			byString.put(stringPath, path);
		}

		/*
		 * avoid requiring each treenode to have a containing tree ref
		 */
		void putTree(Object containingTree) {
			this.containingTree = containingTree;
		}

		void remove(String path) {
			checkTrace("remove", path);
			byString.remove(path);
		}

		void setChildListCreator(Supplier<List> childListCreator) {
			this.childListCreator = childListCreator;
		}
	}
}
package cc.alcina.framework.common.client.csobjects.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

	public void dump(int depth, int maxDepth) {
		FormatBuilder fb = new FormatBuilder();
		fb.indent(depth * 2);
		fb.append(toString());
		Ax.out(fb.toString());
		if (depth < maxDepth) {
			getChildren().forEach(n -> n.dump(depth + 1, maxDepth));
		}
	}

	public TreePath<T> ensureChild(Object segment,
			Comparable segmentComparable) {
		paths.segmentComparable = segmentComparable;
		return ensureChildPath(segment);
	}

	public TreePath<T> ensureChildPath(Object segment) {
		return ensurePath(toString() + "." + asSegment(segment));
	}

	public TreePath<T> ensurePath(String stringPath) {
		return paths.path(stringPath);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TreePath && toString().equals(obj.toString());
	}

	public <E extends Entity> E find(Class<E> clazz) {
		return Domain.find(clazz, segmentAsLong());
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
		paths.remove(toString());
		parent.children.remove(this);
		parent.recalculateCount();
		parent = null;
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

	private List<TreePath<T>> ensureChildren() {
		if (children == null) {
			children = paths.createChildList();
		}
		return children;
	}

	private long segmentAsLong() {
		return Long.parseLong(getSegment());
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

	public static class DepthSegmentComparator
			implements Comparator<TreePath<?>> {
		static final SegmentComparator INSTANCE = new SegmentComparator();

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
			return SegmentComparator.INSTANCE.compare(c1, c2);
		}
	}

	@Reflected
	public static enum Operation {
		INSERT, CHANGE, REMOVE;
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

		public T current() {
			return this.current.getValue();
		}

		public Walker(TreePath<T> from) {
			current = from;
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
	}

	static class Paths {
		public Comparable segmentComparable;

		TreePath root;

		Map<String, TreePath> byString = new LinkedHashMap<>();

		Object containingTree;

		Supplier<List> childListCreator = () -> new ArrayList();

		boolean trace;

		public Paths(TreePath root) {
			this.root = root;
			put(root);
		}

		public void clearNonRoot() {
			byString.entrySet().removeIf(e -> e.getValue() != root);
		}

		public List createChildList() {
			return childListCreator.get();
		}

		public boolean hasPath(String stringPath) {
			return byString.containsKey(stringPath);
		}

		public TreePath path(String stringPath) {
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

		/*
		 * avoid requiring each treenode to have a containing tree ref
		 */
		public void putTree(Object containingTree) {
			this.containingTree = containingTree;
		}

		public void remove(String path) {
			checkTrace("remove", path);
			byString.remove(path);
		}

		public void setChildListCreator(Supplier<List> childListCreator) {
			this.childListCreator = childListCreator;
		}

		private void checkTrace(String op, String path) {
			if (!GWT.isClient()) {
				if (trace) {
					Ax.out("treePath:%s:%s", op, path);
				}
			}
		}

		void put(TreePath path) {
			String stringPath = path.toString();
			if (!byString.containsKey(stringPath)) {
				checkTrace("add", stringPath);
			}
			byString.put(stringPath, path);
		}
	}
}
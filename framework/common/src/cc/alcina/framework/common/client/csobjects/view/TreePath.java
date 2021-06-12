package cc.alcina.framework.common.client.csobjects.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 * This could be implemented with each treepath having a Map<String,Child> - but this way seems a lot slimmer
 */
public class TreePath<T> extends Model {
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

	private transient List<TreePath<T>> children;

	private transient String cached;

	private transient T value;

	private String segment = "";

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
		addChildPath(index, child);
		return child;
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

	public TreePath<T> ensureChild(Object segment) {
		TreePath<T> childPath = ensureChildPath(segment);
		return childPath != null ? childPath : addChild(segment);
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

	@AlcinaTransient
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

	public void reinsertInParent() {
		TreePath<T> parent = getParent();
		removeFromParent();
		parent.addChildPath(parent.getChildren().size(), this);
	}

	public void removeFromParent() {
		paths.byString.remove(toString());
		parent.children.remove(this);
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

	public void setValue(T value) {
		T old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	public void sortChildren() {
		Preconditions.checkState(children instanceof SortedChildren);
		SortedChildren<T> sortedChildren = (SortedChildren<T>) children;
		sortedChildren.reSort();
	}

	@Override
	public String toString() {
		if (cached == null) {
			cached = parent == null || parent.toString().isEmpty() ? segment
					: parent.toString() + "." + segment;
		}
		return cached;
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

	@ClientInstantiable
	public static enum Operation {
		INSERT, REMOVE, CHANGE;
	}

	static class Paths {
		TreePath root;

		Map<String, TreePath> byString = new LinkedHashMap<>();

		Object containingTree;

		Supplier<List> childListCreator = () -> new ArrayList();

		public Paths(TreePath root) {
			this.root = root;
			put(root);
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
					return treePath;
				}
				int segmentStart = cursor.length() + 1;
				idx = stringPath.indexOf(".", segmentStart);
				cursor = idx == -1 ? stringPath : stringPath.substring(0, idx);
				String segment = cursor.substring(segmentStart,
						cursor.length());
				treePath = treePath.addChild(segment);
			}
		}

		/*
		 * avoid requiring each treenode to have a containing tree ref
		 */
		public void putTree(Object containingTree) {
			this.containingTree = containingTree;
		}

		public void setChildListCreator(Supplier<List> childListCreator) {
			this.childListCreator = childListCreator;
		}

		void put(TreePath path) {
			byString.put(path.toString(), path);
		}
	}
}
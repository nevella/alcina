package cc.alcina.framework.common.client.csobjects.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 * This could be implemented with each treepath having a Map<String,Child> - but this way seems a lot slimmer
 */
public class TreePath<T> extends Model {
	public static TreePath absolutePath(String path) {
		Preconditions.checkArgument(path.length() > 0);
		TreePath<?> root = root(path.split("\\.")[0]);
		return root.atPath(path);
	}

	public static <T> TreePath<T> root(Object rootSegment) {
		TreePath root = new TreePath();
		root.withSegment(rootSegment);
		root.paths = new Paths(root);
		return root;
	}

	private transient Paths paths;

	private TreePath<T> parent;

	private transient List<TreePath<T>> children = new ArrayList<>();

	private transient String cached;

	private transient T value;

	private String segment = "";

	private int initialIndex;

	// Should only be used by serialization
	public TreePath() {
	}

	public TreePath<T> addChild(Object segmentObject) {
		return addChild(segmentObject, children.size());
	}

	public TreePath<T> addChild(Object segmentObject, int index) {
		TreePath<T> child = new TreePath();
		child.setSegment(asSegment(segmentObject));
		child.setParent(this);
		child.paths = paths;
		paths.put(child);
		if (index == children.size()) {
			children.add(child);
		} else {
			children.add(index, child);
		}
		child.setInitialIndex(index);
		return child;
	}

	public TreePath<T> atPath(String stringPath) {
		return paths.path(stringPath);
	}

	public TreePath<T> childPath(Object segment) {
		return atPath(toString() + "." + asSegment(segment));
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

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TreePath && toString().equals(obj.toString());
	}

	@AlcinaTransient
	public List<TreePath<T>> getChildren() {
		return this.children;
	}

	public int getInitialIndex() {
		return this.initialIndex;
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

	@Override
	public int hashCode() {
		return toString().hashCode();
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

	public int provideCurrentIndex() {
		if (parent == null) {
			return 0;
		} else {
			return parent.getChildren().indexOf(this);
		}
	}

	public boolean provideIsEmpty() {
		return toString().isEmpty();
	}

	public void putTree(Object containingTree) {
		paths.putTree(containingTree);
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

	public void setInitialIndex(int initialIndex) {
		this.initialIndex = initialIndex;
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

	@Override
	public String toString() {
		if (cached == null) {
			cached = parent == null || parent.toString().isEmpty() ? segment
					: parent.toString() + "." + segment;
			Ax.out(cached);
		}
		return cached;
	}

	public TreePath withSegment(Object object) {
		segment = asSegment(object);
		return this;
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

	@ClientInstantiable
	public static enum Operation {
		INSERT, REMOVE, CHANGE;
	}

	static class Paths {
		TreePath root;

		Map<String, TreePath> byString = new LinkedHashMap<>();

		Object containingTree;

		public Paths(TreePath root) {
			this.root = root;
			put(root);
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

		void put(TreePath path) {
			byString.put(path.toString(), path);
		}
	}
}
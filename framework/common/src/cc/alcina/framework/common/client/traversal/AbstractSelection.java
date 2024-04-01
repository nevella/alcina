package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasFilterableString;

public abstract class AbstractSelection<T> implements Selection<T> {
	T value;

	Node node;

	String pathSegment;

	List<String> filterableSegments = new ArrayList<>();

	int segmentCounter = -1;

	volatile Selection.Relations relations;

	@Override
	public boolean hasRelations() {
		return relations != null;
	}

	public AbstractSelection(Node parentNode, T value, String pathSegment) {
		if (!(this instanceof AllowsNullValue)) {
			Preconditions.checkNotNull(value);
		}
		this.value = value;
		this.node = parentNode.add(this);
		if (pathSegment == null) {
			pathSegment = parentNode.tree().createUniqueSegment(this);
		}
		setPathSegment(pathSegment);
	}

	@Override
	public Relations getRelations() {
		if (relations == null) {
			synchronized (this) {
				if (relations == null) {
					relations = new Relations(this);
				}
			}
		}
		return relations;
	}

	public AbstractSelection(Selection parent, T value) {
		this(parent.processNode(), value, null);
	}

	/*
	 * Use this if collisions/duplicate selections are possible (pathSegment
	 * defines equivalence). Otherwise use the constructor *without* a
	 * pathsegment
	 */
	public AbstractSelection(Selection parent, T value, String pathSegment) {
		this(parent.processNode(), value, pathSegment);
	}

	protected String contentsToString() {
		T t = get();
		if (t instanceof DomNode) {
			return "[DomNode]";
		} else {
			return t.toString();
		}
	}

	public int ensureSegmentCounter() {
		if (segmentCounter == -1) {
			segmentCounter = Integer.parseInt(pathSegment);
		}
		return segmentCounter;
	}

	@Override
	public T get() {
		return value;
	}

	@Override
	public List<String> getFilterableSegments() {
		return this.filterableSegments;
	}

	@Override
	public String getPathSegment() {
		return this.pathSegment;
	}

	@Override
	public Node processNode() {
		return node;
	}

	public void set(T value) {
		this.value = value;
	}

	public void setPathSegment(String pathSegment) {
		Preconditions.checkState(this.pathSegment == null);
		this.pathSegment = pathSegment;
		if (pathSegment != null) {
			filterableSegments.add(pathSegment);
		}
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", getPathSegment(),
				get() == null ? null : Ax.trim(contentsToString(), 150));
	}

	public interface AllowsNullValue {
	}

	Selection.View view;

	@Override
	public Selection.View view() {
		if (view == null) {
			view = Registry.impl(Selection.View.class, getClass());
		}
		return view;
	}

	protected static class View<S extends AbstractSelection>
			implements Selection.View<S> {
		String filterableString;

		public String getText(S selection) {
			if (filterableString == null) {
				filterableString = HasFilterableString
						.filterableString(selection.get());
			}
			return filterableString;
		}
	}
}
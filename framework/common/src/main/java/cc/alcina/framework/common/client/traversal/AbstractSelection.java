package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.Preconditions;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.util.Ax;

public abstract class AbstractSelection<T> implements Selection<T> {
	T value;

	Node node;

	String pathSegment;

	List<String> filterableSegments = new ArrayList<>();

	int segmentCounter = -1;

	volatile Selection.Relation relations;

	public boolean hasReplacedBy() {
		return hasRelations() && relations.hasReplacedBy();
	}

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
	public Relation getRelations() {
		if (relations == null) {
			synchronized (this) {
				if (relations == null) {
					relations = new Relation(this);
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
		return Ax.trimForLogging(get().toString());
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

	SelectionView.View view;

	@Override
	public SelectionView.View view() {
		if (view == null) {
			view = Registry.impl(SelectionView.View.class, getClass());
		}
		return view;
	}

	SelectionView.RowView rowView;

	@Override
	public SelectionView.RowView rowView() {
		if (rowView == null) {
			rowView = Registry.impl(SelectionView.RowView.class, getClass());
			rowView.putSelection(this);
		}
		return rowView;
	}
}
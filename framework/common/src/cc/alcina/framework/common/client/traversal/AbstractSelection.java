package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.util.Ax;

public abstract class AbstractSelection<T> implements Selection<T> {
	private T value;

	private Node node;

	private String pathSegment;

	private List<String> filterableSegments = new ArrayList<>();

	private int segmentCounter = -1;

	public int ensureSegmentCounter() {
		if (segmentCounter == -1) {
			segmentCounter = Integer.parseInt(pathSegment);
		}
		return segmentCounter;
	}

	public AbstractSelection(Node parentNode, T value, String pathSegment) {
		if (!(this instanceof HasNullValue)) {
			Preconditions.checkNotNull(value);
		}
		this.value = value;
		this.node = parentNode.add(this);
		if (pathSegment == null) {
			pathSegment = parentNode.tree().createUniqueSegment(this);
		}
		setPathSegment(pathSegment);
	}

	public interface HasNullValue {
	}

	/*
	 * Use this if collisions/duplicate selections are possible (pathSegment
	 * defines equivalence). Otherwise use the constructor *without* a
	 * pathsegment
	 */
	public AbstractSelection(Selection parent, T value, String pathSegment) {
		this(parent.processNode(), value, pathSegment);
	}

	public AbstractSelection(Selection parent, T value) {
		this(parent.processNode(), value, null);
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

	protected String contentsToString() {
		T t = get();
		if (t instanceof DomNode) {
			return "[DomNode]";
		} else {
			return t.toString();
		}
	}

	static class View implements Selection.View<AbstractSelection> {
	}
}
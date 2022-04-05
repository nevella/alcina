package cc.alcina.framework.servlet.traversal;

import cc.alcina.framework.servlet.job.TreeProcess.Node;

public abstract class AbstractSelection<T> implements Selection<T> {
	private T value;

	private Node node;

	private String pathSegment;

	public AbstractSelection(Node parentNode, T value, String pathSegment) {
		this.value = value;
		this.pathSegment = pathSegment;
		this.node = parentNode.add(this);
	}

	public AbstractSelection(Selection parent, T value, String pathSegment) {
		this(parent.processNode(), value, pathSegment);
	}

	@Override
	public T get() {
		return value;
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

	@Override
	public String toString() {
		return getPathSegment();
	}
}
package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.servlet.job.TreeProcess.Node;

public abstract class AbstractSelection<T> implements Selection<T> {
	private T value;

	private Node node;

	public AbstractSelection(Node parentNode, T value) {
		this.value = value;
		this.node = parentNode.add(this);
	}

	public AbstractSelection(Selection parent) {
		this(parent.processNode(), null);
	}

	public AbstractSelection(Selection parent, T value) {
		this(parent.processNode(), value);
	}

	@Override
	public T get() {
		return value;
	}

	@Override
	public Node processNode() {
		return node;
	}

	public void set(T value) {
		this.value = value;
	}
}
package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.log.TreeProcess.Node;

public abstract class AbstractSelection<T> implements Selection<T> {
	private T value;

	private Node node;

	private String pathSegment;

	private List<String> filterableSegments = new ArrayList<>();

	public AbstractSelection(Node parentNode, T value, String pathSegment) {
		this.value = value;
		this.pathSegment = pathSegment;
		this.node = parentNode.add(this);
		filterableSegments.add(pathSegment);
	}

	public AbstractSelection(Selection parent, T value, String pathSegment) {
		this(parent.processNode(), value, pathSegment);
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

	@Override
	public String toString() {
		return getPathSegment();
	}
}
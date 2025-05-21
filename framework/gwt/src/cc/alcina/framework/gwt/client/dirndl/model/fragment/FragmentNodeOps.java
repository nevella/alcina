package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/*
 * Utility operations on FragmentNode-like classes
 */
public interface FragmentNodeOps {
	default <N extends FragmentNode> Stream<N> byType(Class<N> clazz) {
		return (Stream<N>) stream().filter(n -> n.getClass() == clazz);
	}

	default <N extends FragmentNode> Stream<N>
			byTypeAssignable(Class<N> clazz) {
		return (Stream<N>) stream()
				.filter(n -> Reflections.isAssignableFrom(clazz, n.getClass()));
	}

	default <N extends FragmentNode> List<N> byTypeList(Class<N> clazz) {
		return byType(clazz).collect(Collectors.toList());
	}

	default <N extends FragmentNode> N byTypeNode(Class<N> clazz) {
		return byType(clazz).findFirst().orElse(null);
	}

	default <N extends FragmentNode> Optional<N>
			byTypeOptional(Class<N> clazz) {
		return byType(clazz).findFirst();
	}

	Stream<? extends FragmentNode> children();

	default List<? extends FragmentNode> childList() {
		return children().toList();
	}

	void ensureComputedNodes();

	default Stream<? extends FragmentNode> stream() {
		return (Stream<? extends FragmentNode>) (Stream) new DepthFirstTraversal<FragmentNodeOps>(
				this, fn -> fn.children().collect(Collectors.toList())).stream()
						.filter(n -> n instanceof FragmentNode);
	}

	default Stream<? extends FragmentNode> descendants() {
		return stream().filter(n -> n != this);
	}

	String toStringTree();

	default List<Class<? extends FragmentNode>> types() {
		return stream().map(FragmentNode::getClass).distinct()
				.collect(Collectors.toList());
	}
}

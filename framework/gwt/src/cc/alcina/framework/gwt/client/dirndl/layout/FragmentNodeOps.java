package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/*
 * Utility operations on FragmentNode-like classes
 */
public interface FragmentNodeOps {
	default <N extends FragmentNode> Stream<N> byType(Class<N> clazz) {
		return (Stream<N>) stream().filter(n -> n.getClass() == clazz);
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

	default Stream<? extends FragmentNode> stream() {
		return (Stream<? extends FragmentNode>) (Stream) new DepthFirstTraversal<FragmentNodeOps>(
				this, fn -> fn.children().collect(Collectors.toList())).stream()
						.filter(n -> n instanceof FragmentNode);
	}
}

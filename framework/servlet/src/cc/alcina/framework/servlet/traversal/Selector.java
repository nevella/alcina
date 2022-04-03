package cc.alcina.framework.servlet.traversal;

public interface Selector {
	default boolean handles(Selection selection) {
		return true;
	}

	void process(SelectionTraversal selectionTraversal, Selection selection);
}

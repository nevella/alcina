package cc.alcina.framework.common.client.traversal;

/**
 *
 * Transforms I into List<O> - note that O is not used in the API, it's simply a
 * documentation assist
 *
 * If a resource acquired by the selector is not used by descendant selectors
 * (say a document loaded from a url containing a list of child urls), it makes
 * sense to load-process-release in the one selector.
 *
 * Otherwise, load, emit the loaded resource and have the next generation
 * (child) selector do first-stage processing
 *
 *
 */
public interface Selector<I extends Selection, O extends Selection> {
	default boolean handles(Selection selection) {
		return true;
	}

	void process(SelectionTraversal traversal, I selection) throws Exception;
}

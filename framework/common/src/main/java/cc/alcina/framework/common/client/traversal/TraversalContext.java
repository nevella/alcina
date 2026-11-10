package cc.alcina.framework.common.client.traversal;

/*
 * Marker for the root context (peer) object of a traversal
 */
public interface TraversalContext {
	public interface ThrowOnException extends TraversalContext {
	}

	/*
	 * Do not retain for debugging (by default)
	 */
	public interface ShortTraversal extends TraversalContext {
		default boolean provideRetain() {
			return false;
		}
	}

	/*
	 * Do not retain for traversal browser
	 */
	public interface NonDefaultTraversal extends TraversalContext {
	}
}

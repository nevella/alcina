package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 * Custom access for package-external classes. Sort of like Unsafe
 */
public class DirndlAccess {
	/*
	 * Note - only allows access from a component (model) of class x to an
	 * ancestor of exactly the same class - not an arbitrary ancestor
	 */
	public static class ComponentAncestorAccess {
		public static <T extends Model> T getAncestor(Node sourceNode,
				T descendant) {
			Class<T> clazz = (Class<T>) descendant.getClass();
			Node cursor = sourceNode;
			while (cursor != null) {
				if (cursor.model != null && cursor.model.getClass() == clazz) {
					return cursor.getModel();
				}
				cursor = cursor.parent;
			}
			return null;
		}
	}

	public static <A extends Annotation> A parentAnnotation(Node n,
			Class<A> clazz) {
		return n.parent.annotation(clazz);
	}

	public static void appendToNode(DirectedLayout.Node directedNode,
			FragmentNode child) {
		directedNode.append(child);
	}

	/*
	 * Only call from framework code (here, or FragmentModel). If syncing from
	 * mutations, do not double-remove
	 * 
	 * 
	 */
	public static void removeNode(DirectedLayout.Node directedNode,
			boolean willReattach) {
		directedNode.remove(willReattach);
	}
	/**
	 * Nope - use Direc
	 * 
	 * public static Object parentModel(Node n) { return n.parent.model; }
	 * 
	 * public static Object grandparentModel(Node n) { return
	 * n.parent.parent.model; }
	 */
}

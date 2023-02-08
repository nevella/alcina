package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 * Custom access for package-external classes
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
				if (cursor.model.getClass() == clazz) {
					return cursor.getModel();
				}
				cursor = cursor.parent;
			}
			return null;
		}
	}
}

package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;

/**
 * <p>
 * WIP - an "island" in a FragmentModel, which is itself a FragmentRoot. It
 * supports FragmentModels within a parent FragmentModel - such as an annotation
 * within an annotation, or a document within a document.
 * 
 * <p>
 * The fundamental rule is: "if selections which are part-subtree,
 * part-parent-tree should not be editable, make the subtree an isolate "
 * 
 * <p>
 * A FragmentIsolate affects {@link FragmentModel} behaviour as follows:
 * <ul>
 * <li>#addDescent - if an isolate is encountered, the subtree is skipped (for
 * creation), the isolate's model parses the subtree instead
 * <li>#removeDescent - basically the same
 * </ul>
 * <p>
 * A FragmentIsolate affects {@link FragmentNode} behaviour as follows:
 * <ul>
 * <li>#children If FragmentNode N is an isolate, it has no logical children,
 * its <i>peer FragmentModel</i> does - and it's that model which is sorta the
 * blood-brain barrier
 * </ul>
 */
public interface FragmentIsolate {
	/**
	 * This will be a peer object (say an inner class) linked to the
	 * FragmentNode
	 * 
	 * @return the FragmentModel for the isolate subtree
	 */
	FragmentModel getFragmentModel();
}

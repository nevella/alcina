package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import cc.alcina.framework.common.client.reflection.Property;

/**
 * <p>
 * WIP - an "island" in a FragmentNode, which does not have FragmentNode
 * children in the context of the owning FragmentModel. A _descendant_ may
 * itself be a FragmentModel - such as an annotation within an annotation, or a
 * document within a document - but a FragmentIsolate itself may not.
 * 
 * <p>
 * Unlike regular FragmentNodes, FragmentIsolates can contain directed children
 * 
 * <p>
 * A FragmentIsolate affects {@link FragmentModel} behaviour as follows:
 * <ul>
 * <li>#addDescent - if an isolate is encountered, the subtree is skipped (for
 * creation)
 * <li>#removeDescent - basically the same
 * </ul>
 * <p>
 * A FragmentIsolate affects {@link FragmentNode} behaviour as follows:
 * <ul>
 * <li>#children If FragmentNode N is an isolate, it has no logical children
 * </ul>
 */
public interface FragmentIsolate {
}

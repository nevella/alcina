package com.google.gwt.dom.client.mutations;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

/**
 * <p>
 * A mutation is assigned to a mutationgroup (there are at least two mutations
 * per group) if the cumulative effect of the mutations is no modification to
 * the character sequence of the document
 * 
 * <p>
 * Examples are splitting a text node and wrapping any node. The primary use
 * case is to preserve the text index for Location instances, even when they're
 * within the text node being mutated; this extra modelling also allows
 * preservation of attachIds (and elides unbind/bind cycles) for wrapped nodes.
 * <p>
 * Note that {@link #merge} is unused - since the post-split locations will be
 * treeindex-mutated by split, there's no advantage to a merge (and no current
 * {@link DomNode} operation to support it )
 * <p>
 * Split requires no special attach/detach handling - where strip and wrap do
 * (see {@link Document#getWillReattach} )
 */
@Reflected
public enum MutationGroup {
	split, wrap, merge, strip
}

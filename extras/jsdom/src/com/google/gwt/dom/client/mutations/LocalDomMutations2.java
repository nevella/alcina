package com.google.gwt.dom.client.mutations;

/**
 * <h3>Goals</h3>
 * <ul>
 * <li>Completely correct NodeLocal structure at all time
 * <ul>
 * <li>Track all mutations outside blessed ({@code LocalDom.flush},
 * {@code NodeRemote.mutate()}} methods.
 * <li>Optionally: LocalDom.invokeExternal becomes a no-op (if trackAllMutations
 * is on)
 * <li>Create an in-client verifier that will generate nodetree dumps - a
 * nodetree being an object tree modelling the dom that can be compared, used
 * for debugging etc
 * </ul>
 * <li>Model mutations nicely
 * <ul>
 * <li>MutationRecord jso -&gt; MutationRecordRemote
 * <li>Java wrapper has a nice tostring
 * <li>Java wrapper is for tracking the 'apply to local' algorithm
 * </ul>
 * <li>Cleanup remote mutation
 * <ul>
 * <li>Probably each op should call through a pipeline/router ('appendChild'
 * calls 'mutate(()->appendChild)' - can ignore if in mutation replay mode
 * </ul>
 * <li>Mutation application sketch: (assume mutations are of type childList)
 * <ul>
 * <li>Case 1: 1 mutation [OP1]
 * <ul>
 * <li>Parent noderemote will be attached to the local dom. Get that node. (N1)
 * <li>If they exist (pre, post) sibling will be attached to the dom. Get that
 * (if not a sole child). (N2)
 * <ul>
 * <li>Removal: determine the mutated node from the tuple [N1,N2,OP1] - N3.
 * Apply remove
 * <li>Addition: create, apply to [N1, N2]
 * </ul>
 * </ul>
 * </ul>
 * <ul>
 * <li>Case 2: 2 mutations [OP1, OP2]
 * <ul>
 * <li>...TBD
 * </ul>
 * </ul>
 * </ul>
 * </ul>
 *
 * <p>
 * Endfile
 * </p>
 *
 * @author nick@alcina.cc
 *
 */
public class LocalDomMutations2 {
}

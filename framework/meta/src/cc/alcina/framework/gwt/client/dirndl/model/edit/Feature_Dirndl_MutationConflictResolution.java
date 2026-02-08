package cc.alcina.framework.gwt.client.dirndl.model.edit;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;

/**
 * <h3>Timelines, editors and the rom</h3>
 * 
 * <p>
 * Goals:
 *
 * <ul>
 * <li>Client edits always win
 * <li>Server changes are 'rejected pending merge' by ths client
 * </ul>
 */
/*
@formatter:off

Timeline example (sequence is somewhat fuzzy, since client + server are on different worldlines):

Client: C
Server: S
Dom(env)(index): Dc.1 etc
Mutation(env)(index): Mc.1 etc

T0 - Mc.1 Client edit of say a choice editor contenteditable - value is 'a'
T1 - Mc.1 -> S; Ms.1 Server mutation (in response) - wraps 'a' in a SuggestinngNode, attaches an Overlay
T2 - Mc.2 Client continues edit -  value is 'ar'
T3 - Ms.1 -> C: Rc.1 - Client rejects the mutation, sends rejection message. 
                 Note that the server doesn't need the message per se (since it will deduce one)
T4 - Mc2 ->s; Ms.2 Server computes that the client will have rejected Ms.1, so synthesizes a rejection message
     Rejection/reconciliation (MutationConflictMerger):
	   - MCM marks Ms.1 as rejected
	   - MCM begins Ms.2 with mutations from Ms.1
	   - MCM undoes _only part_ of Ms.1 (the part affecting the content editable)	 
	   - MCM causes mutation cascade of undo (which will undo the overlay). Note queueing here - 
	   - Note that mutationrecord normalisation will remove add/remove pairs
	   - Note that at no point is Ds inconsistent - we're just mutating it in a more complex way to maintain 'client edits always win'
	   - Mc2 applied as normal


Test:
- force delay handling of 






@formatter:on


*/
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_Dirndl_MutationConflictResolution extends Feature {
}

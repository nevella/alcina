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
 * </ul>
 *
 * <pre>
 * <code>

 Notation:

 S0 - state before mutation 0
 SN - state after mutation n (end of mutation list)

 R(a...z) - Remote nodes (at any time in the mutation sequence)
 L(0...9) - Local nodes (at S0)
 L(Ra)    - Local node corresponding to Ra

 I(a...z)     - Invert node (see below) corresponding to Ra

 M[op{A,R},node,parent,predecessor] - a mutation
 Inv[M] - the inverse mutation

 Goal: apply mutations to the graph of NodeLocal nodes (the local dom)

 Required: for transform T0 := M[Ra,Rb,Rc] at S0, what the nodes L(Ra), L(Rb), L(Rc)

 Assume: no knowledge of any correspondence between any Lx and Rx at S0

 -----------------------

 Step 1: Build a 'InverseTree' of "InverseNodes' by iterating over the childlist mutations in reverse order, reversing the operation
 *and including knowledge of parents and siblings*

 This builds one or more disjunct subtrees modelling the 'state of the R nodes' at S0. It won't be a complete model of the
 subtree, but it *will* be internally consistent. An example:

         L0::Ra   [Ra's parent does not change, so the L0 :: Ra correspondence is trivially computable post-mutation]
          /  \
         /    \
      L1::Rb  L2::Rc
       /  \
      /    \
 L3::Rd    L4::Re

Mutations:
M0 - [R,Re,Rb,Rd]
M1 - [R,Rb,Ra,--]

Build inverse tree:

I[M1] ::
	  E[Ra]
	    |
	    |
	  E[Rb]

I[M0] ::
          E[Ra]
          /  \
         /    \
      E[Rb]   ?? (could determine from M0.successor but don't think it's needed
       /  \
      /    \
 E[Rd]    E[Re]

Step 2: Descending from known correspondences (L0::Ra in the example), determine L0::R? correspondence

For node E[Rb]:

Case 1: (satisfied in the example) If E[Rb] has no predecessor, then Rb corresponds to the first child of L0, i.e. L1

Case 2: Say we removed Rc instead - so E[Rc] had a predecessor, say E[Rb]. What was the child index of Rc in R0 at S0?
(We don't know that Rb had no predecessor)

Form the InverseFinalList of children of Ra at Sn:

Ria Rib Ric...Riz

Form the InverseStartList (partial) of children of Ra at S0 (node that sequences may not be in order):

?? - {E[Rb] - E[Rc]} - ??

Goal is to form the RemoteStartList (Rja...Rjz) of remote children of Ra at S0. Begin by making it a copy of the InverseFinalList

LocalStartList (Lka...Lky) is the list of local children of L0 at S0

Any nodes R that were mutated during M, are children of Ra at SN but *not* in the InverseStartList
 were *not* children of Ra at S0 :: remove from the RemoteStartList.

Length of the RemoteStartList should now equal the length of the LocalStartList.

Now all that remains is to order RemoteStartList. Note that the relative positions of the ?? nodes did not change during mutation.

So iterate through the RemoteStartList. For node Rjx ::

- Is it in (does it correspond to) the inversestartlist? If so, order it relative to its position in the partial
- Otherwise leave unchanged

The rest is just induction

m
*
*
*




 * </code
 * </pre>
 *
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
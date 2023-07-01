package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.Mutation;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 *
 * <p>
 * A peer to a {@link Model} instance that maintains sync between its child
 * structure and another object tree via localdom mutations
 *
 * <ul>
 * <li>It subscribes to a localdom mutation event source
 * <ul>
 * <li>The event source (like the DOM mutationlistener) collects events and
 * emits as batches on request, rather than per mutation
 * <li>In client code, batch handling is automatic via scheduleFinally()
 * <li>In server code, batch handling must be triggered via a call to
 * LocalDom.mutations().emitMutations()
 * </ul>
 * <li>The model converts dommutations into changes to its model structure,
 * which are themselves batched and emitted (on localdom mutation processing
 * completion) as modelmutation event collations.
 * <li>The model also maintains indicies (child models by type, etc)
 * </ul>
 * <p>
 * TODO
 * <ul>
 * <li>Remove DirectedLayout.Node.widget dep - instead switch to Rendered which
 * wraps an org.w3c.dom.Node, has getWidget() etc (done)
 * <li>Emit localdom local dom mutation events (as DOM mutations)
 * <li>Emit dom mutation events as FragmentNode model mutation events
 * <li>FragmentModel maintains indicies of models (by class etc)
 * <li>Emit mutationcollation events (as finally events)
 *
 * </ul>
 *
 *
 * @author nick@alcina.cc
 *
 */
@Feature.Ref(Feature_Dirndl_FragmentModel.class)
public class FragmentModel implements InferredDomEvents.Mutation.Handler {
	Model model;

	public FragmentModel(Model peer) {
		this.model = peer;
	}

	@Override
	public void onMutation(Mutation event) {
		int debug = 3;
	}
}

package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 *
 * <p>
 * Tracks FragmentModel - sync between dom and model tree
 *
 * <p>
 * TODO
 * <ul>
 * <li>Remove DirectedLayout.Node.widget dep - instead switch to Rendered which
 * wraps an org.w3c.dom.Node, has getWidget() etc
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
public class FragmentModel extends Model {
}

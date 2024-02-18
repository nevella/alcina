package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 * <h4>Show the layers, input and output of a selection traversal</h4>
 *
 *
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_TraversalProcessView extends Feature {
	/**
	 * <pre>
	 * - show shortcuts/commands
	 * - show documentation
	 * - default to filtering of selection
	 * </pre>
	 */
	@Feature.Type.Ref(Feature.Type.Ui_implementation.class)
	@Feature.Parent(Feature_TraversalProcessView.class)
	public interface AppSuggestorImplementation extends Feature {
	}
}

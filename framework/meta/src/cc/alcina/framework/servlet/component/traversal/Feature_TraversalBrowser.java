package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_feature;
import cc.alcina.framework.gwt.client.dirndl.cmp.Feature_StatusModule;

/**
 * <h4>Show the layers, input and output of a selection traversal</h4>
 *
 *
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_feature.class)
public interface Feature_TraversalBrowser extends Feature {
	/**
	 * This feature is basically to have somewhere to hang Alcina status module
	 * tests
	 */
	@Feature.Parent(Feature_TraversalBrowser.class)
	@Feature.Ref(Feature_StatusModule.class)
	public interface _StatusModule extends Feature {
	}
}

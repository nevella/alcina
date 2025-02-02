package cc.alcina.framework.servlet.component.entity;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_app;

/**
 * <h4>A subtype of traversal browser, a quick entity graph traversal tool</h4>
 *
 *
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_app.class)
public interface Feature_EntityBrowser extends Feature {
	/*
	 * A minimal query cache for full-table queries - useful for deeper
	 * navigation
	 * 
	 * This cache can be cleared via the ui area
	 */
	@Feature.Parent(Feature_EntityBrowser.class)
	public interface _QueryCache extends Feature {
	}
}

package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.common.client.meta.Feature;

@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_Dirndl_Documentation extends Feature {
	/**
	 * Comparison (implementation) with React, Flutter, Swift.UI
	 *
	 * 
	 *
	 */
	@Feature.Status.Ref(Feature.Status.Open.class)
	@Feature.Parent(Feature_Dirndl_Documentation.class)
	interface Comparison extends Feature {
	}

	/**
	 * A cookbook of Dirndl recipes
	 *
	 * see
	 * alcina/framework/gwt/src/cc/alcina/framework/gwt/client/dirndl/layout/doc/cookbook.html
	 *
	 * 
	 *
	 */
	@Feature.Status.Ref(Feature.Status.In_Progress.class)
	@Feature.Parent(Feature_Dirndl_Documentation.class)
	interface Cookbook extends Feature {
	}

	/**
	 * A sandbox for toy Dirndl examples
	 *
	 * 
	 *
	 */
	@Feature.Status.Ref(Feature.Status.Open.class)
	@Feature.Parent(Feature_Dirndl_Documentation.class)
	interface Sandbox extends Feature {
	}
}

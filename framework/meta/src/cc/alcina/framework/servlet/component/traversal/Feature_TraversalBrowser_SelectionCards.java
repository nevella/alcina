package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * <p>
 * The main selections area
 * 
 *
 */
@Feature.Parent(Feature_TraversalBrowser.class)
public interface Feature_TraversalBrowser_SelectionCards extends Feature {
	/**
	 * <p>
	 * Filter the current layer by x (initially text - the layer selection type
	 * would have to provide properties)
	 * 
	 * <p>
	 * Test sketch: alcina/croissanteria - click filter - filter 'milk'
	 */
	@Feature.Parent(Feature_TraversalBrowser.class)
	public interface Layer_filter extends Feature {
	}

	/**
	 * <p>
	 * Perf - ensure that components aren't double-rendered due to
	 * propertychange cascade
	 * <ul>
	 * <li>add logging for major components; fire when created
	 * <li>log place; history change. verify that each change only causes one
	 * component re-render
	 * <li>For layers, work on collection changes not causing full re-render
	 * <li>Check mutation bandwidth
	 * 
	 * </ul>
	 */
	@Feature.Parent(Feature_TraversalBrowser.class)
	@Feature.Type.Ref(Feature.Type.Ui_implementation.class)
	public interface Performance extends Feature {
	}
}

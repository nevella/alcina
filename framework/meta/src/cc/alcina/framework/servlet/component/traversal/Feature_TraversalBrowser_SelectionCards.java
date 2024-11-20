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
	@Feature.Parent(Feature_TraversalBrowser_SelectionCards.class)
	public interface Layer_filter extends Feature {
	}
}

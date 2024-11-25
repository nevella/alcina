package cc.alcina.framework.gwt.client.dirndl.overlay;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;
import cc.alcina.framework.gwt.client.dirndl.model.suggest.Suggestor;

/**
 * Tracks improvements to overlay/popups, particularly {@link Suggestor}
 * containers
 *
 */
/*
 * @formatter:off
 * (breakout)
- dropdown max height…
- dropdown show above logical parent if <x px from bottom
- dropdown shows 'has more' (js, resizeobserver) (later/design)
- suggestor/selected choice marker
- show full list (all choices) on focus

 * @formatter:on
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_Overlay extends Feature {
	/**
	 * Tracks fixes to overlay code when the parent is fixed/sticky
	 */
	@Feature.Parent(Feature_Overlay.class)
	public interface _FixedPositioning extends Feature {
	}
}

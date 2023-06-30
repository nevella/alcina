package cc.alcina.framework.gwt.client.dirndl.overlay;

import cc.alcina.framework.common.client.meta.Feature;
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
public interface Feature_Overlay extends Feature {
}

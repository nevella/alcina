/**
 * <p>
 * This component renders an application help system - normally the content is a
 * <em>told</em> story.
 * <p>
 * Two notes re naming, "a tale ... full of sound and fury" and the aussie kids'
 * usage in the early '80s of "untold"...pretty dan great. So I _really_ wanted
 * to call this package/component "told" - but it ain't really
 * <h3>Implementation notes</h3>
 * <p>
 * The main (Area) component can either be a transformation of the application's
 * main {@link com.google.gwt.activity.shared.Activity} topic/place, or a
 * secondary/fragment topic/place (for in-app documentation).
 * <p>
 * See {@link cc.alcina.framework.gwt.client.place.BasePlace} for info about
 * fragment places
 */
@Feature.Parent(Feature_HelpModule.class)
package cc.alcina.framework.gwt.client.dirndl.cmp.help;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.cmp.Feature_HelpModule;

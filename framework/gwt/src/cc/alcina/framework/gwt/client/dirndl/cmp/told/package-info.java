/**
 * <p>
 * This component renders a <em>told</em> story - since those told stories are
 * intended as the complete content of the application help system, that also
 * means "this component renders the help system".
 * <p>
 * Two notes re naming, "a tale ... full of sound and fury" and the aussie kids'
 * usage in the early '80s of "untold"...pretty dan great.
 * <p>
 * Also - the place is serialized as 'help' - clearer to users, and told looks
 * very much like TO_ID (not TO_LD)
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
package cc.alcina.framework.gwt.client.dirndl.cmp.told;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.cmp.Feature_HelpModule;

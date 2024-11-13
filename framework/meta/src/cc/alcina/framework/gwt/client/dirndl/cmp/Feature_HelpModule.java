package cc.alcina.framework.gwt.client.dirndl.cmp;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;

/**
 * <p>
 * The in-UI help
 * <p>
 * Sketch
 * <ul>
 * <li>Interface: provider - provides essentially story nodes + kids
 * <li>UI can be overlay or another window. It accepts a PlaceFragment (new
 * thing)
 * <li>BasePlace.PlaceFragment - a double-slash is a fragment separator -
 * baseplace provides getplacefragments -
 * <li>DirectedActivity handler fires secondary activities (from fragments) on a
 * different topic
 * </ul>
 * 
 */
/*
 * FIXME - romcom - gwt module goes away (although module concept may staty -
 * models + places + events)
 * 
 * Test - alc/traversal ui demo test
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_HelpModule extends Feature {
}

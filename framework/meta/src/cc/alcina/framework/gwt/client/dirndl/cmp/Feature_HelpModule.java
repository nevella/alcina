package cc.alcina.framework.gwt.client.dirndl.cmp;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.layout.Feature_Dirndl;

/**
 * <p>
 * The in-UI help
 * <p>
 * Sketch
 * <ul>
 * <li>Interface: provider - provides essentially story nodes + kids as a
 * structuredcontent object
 * <li>The UI can be an overlay or another window. If an overlay,
 * routing/history is modelled in a BasePlace.fragments element
 * </ul>
 * 
 */
/*
 * FIXME - romcom - gwt module goes away, initial load speed is better handled
 * with romcom (although module concept may staty - models + places + events)
 * 
 * Test - alc/traversal ui demo test
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Parent(Feature_Dirndl.class)
public interface Feature_HelpModule extends Feature {
	/**
	 * The help area - see {@link Feature_ContentBrowserModule._Area} for
	 * implementation docs
	 */
	@Feature.Parent(Feature_HelpModule.class)
	public interface _Area extends Feature {
	}

	/**
	 * <p>
	 * The help system loads content (initially the whole tree) from a provider.
	 * For Alcina romcom apps, this will be a simple fs deserialization of a
	 * Story traversal record.
	 * 
	 * <p>
	 * TODO
	 * <ul>
	 * <li>Header content: App title, doc info
	 * <li>Nav content: structure
	 * <li>Search support: api/results
	 * <li>Content element (element - element + descendants :: rewrites images
	 * based on resolution)
	 * </ul>
	 */
	@Feature.Parent(Feature_HelpModule.class)
	public interface _ContentProviders extends Feature {
	}
}

package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * <h4>A tabular view of selection data (generally the children of the current
 * selection, such as entity children of a property with Set&lt;? extends
 * Entity&gt; type)</h4>
 * 
 *
 */
/*
 * Test: locate + click the dotburger icon in the header
 * 
 * Doc: Short form: 'Click to display a list of keyboard shortcuts'
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_TraversalBrowser.class)
public interface Feature_TraversalBrowser_SelectionTable extends Feature {
	@Feature.Parent(Feature_TraversalBrowser.class)
	public interface Column_sort extends Feature {
	}

	/**
	 * <p>
	 * This is implemented and works for the Jade entity browser
	 * <p>
	 * Generalising to a TraversalBrowser implementation wouldn't be hard
	 * (essentially just abstracting the <code>EntityAnswers</code>
	 * parser/property matcher implementation)
	 */
	@Feature.Status.Ref(Feature.Status.Open_and_interesting.class)
	@Feature.Parent(Feature_TraversalBrowser.class)
	public interface Column_filter extends Feature {
	}
}

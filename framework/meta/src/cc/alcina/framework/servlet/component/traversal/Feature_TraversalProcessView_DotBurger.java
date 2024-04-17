package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * <h4>The dotburger component of the traversal UI header</h4>
 * 
 * <p>
 * The main dropdown navigation/options menu
 *
 * <pre>
 * <code>
	dotburger
	- shortcuts
	- descent mode
		- filter (show all items with descendant-or-self matching the filter  )
		- descent (show all items with descendant-or-self-or-ancestor matching the selected item, or self matching filter  )
		- containment (show all items with descendant-or-self-or-ancestor intersecting the selected item's range, or self matching filter  )
	- pane layout switches
		* i/o documents (if transformation)
		* i doc (if input doc)
 * </code>
 * </pre>
 *
 */
/*
 * Test: locate + click the dotburger icon in the header
 * 
 * Doc: Short form: 'Click to display a list of keyboard shortcuts'
 */
@Feature.Status.Ref(Feature.Status.Open.class)
@Feature.Parent(Feature_TraversalProcessView_Header.class)
public interface Feature_TraversalProcessView_DotBurger extends Feature {
	/*
	 * Test: test the shortcuts term existence, [doc: highlight term + display
	 * doc], click, test the shortcuts pane existence
	 * 
	 * Doc: Short form: 'Click to display a list of keyboard shortcuts'
	 */
	public interface Shortcuts extends Feature {
	}
}

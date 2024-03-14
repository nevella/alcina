package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * <pre>
 * - show shortcuts/commands
 * - show documentation
 * - default to filtering of selection
 * </pre>
 */
@Feature.Type.Ref(Feature.Type.Ui_implementation.class)
@Feature.Parent(Feature_TraversalProcessView.class)
public interface Feature_TraversalProcessView_AppSuggestorImplementation
		extends Feature {
	/**
	 * <h2>Phase 1</h2>
	 * 
	 * <h3>Description</h3>
	 * 
	 * <pre>
	* - Describe the commands ('r' for reload, 'c' to clear the filer)
	 * </pre>
	 * 
	 * <h3>Test</h3>
	 * 
	 * <pre>
	* - Trigger the clear filter, documented
	 * </pre>
	 * 
	 * <h2>Phase 2</h2>
	 * 
	 * <h3>Implement</h3>
	 * 
	 * <pre>
	* - A full keyboard shortcuts list
	- Keyboard shortcut info in commands
	- Suggestor command options (tools icon)
	 * </pre>
	 */
	@Feature.Type.Ref(Feature.Type.Ui_implementation.class)
	@Feature.Parent(Feature_TraversalProcessView_AppSuggestorImplementation.class)
	public interface Shortcuts extends Feature {
	}

	/**
	 * <h3>Description</h3>
	 * 
	 * <pre>
	 * <code>
	- The default suggestor action is to filter the visible selections with the entered text
	- What the entered text matches depends on the selection, but for markup selections it at least 
	includes the markup text content
	- (TODO) also match the selection range if the text is an integer or range
	</code>
	 * </pre>
	 * 
	 * <h3>Test</h3>
	 * 
	 * <pre>
	* <code>
	- Launch the croissanteria app
	- Enter 'flour'
	- Press [enter] (select the 'filter' choice)
	- Test only 'flour' selections are displayed
	</code>
	 * </pre>
	 * 
	 */
	@Feature.Type.Ref(Feature.Type.Ui_implementation.class)
	@Feature.Parent(Feature_TraversalProcessView_AppSuggestorImplementation.class)
	public interface Filter extends Feature {
	}
}
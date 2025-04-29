package cc.alcina.framework.servlet.story.component.gallery;

import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;

/*
 * This tests the MultipleSuggestions gallery page
 */
@Decl.Require(Story_GalleryBrowser.State.Home.class)
@Decl.Child(Point_GalleryBrowser_Home.ToMultipleSuggestions.class)
@Decl.Child(Point_GalleryBrowser_MultipleSuggestions._DefinitionEditor.class)
public class Point_GalleryBrowser_MultipleSuggestions extends Waypoint {
	static final String XPATH_USER_EDITOR = "//definition-editor//multiple-suggestions/edit";

	static final String XPATH_OVERLAY = "//overlay[@class='decorator-suggestor']";

	static final String _REL_EDIT_ENTRY = "//span[@class='cursor-target'][2]";

	@Decl.Child(_DefinitionEditor.ClickSuggestionsArea.class)
	@Decl.Child(_DefinitionEditor.AwaitOverlay.class)
	@Decl.Child(_DefinitionEditor.SendName.class)
	static class _DefinitionEditor extends Waypoint {
		@Decl.Location.Xpath(XPATH_USER_EDITOR)
		@Decl.Action.UI.Click
		static class ClickSuggestionsArea extends Waypoint {
		}

		/*
		 * TODO - this shouldn't be necessary (but currently is)
		 */
		@Decl.Location.Xpath(XPATH_OVERLAY)
		@Decl.Action.UI.AwaitPresent
		static class AwaitOverlay extends Waypoint {
		}

		@Decl.Location.Xpath({ XPATH_USER_EDITOR, _REL_EDIT_ENTRY })
		@Decl.Action.UI.Keys("lar")
		static class SendName extends Waypoint {
		}
	}
}

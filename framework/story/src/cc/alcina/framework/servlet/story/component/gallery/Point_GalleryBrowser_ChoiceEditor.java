package cc.alcina.framework.servlet.story.component.gallery;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.model.edit.Feature_Dirndl_ChoiceEditor;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;

/*
 * This tests the ChoiceEditor gallery page
 */
@Decl.Require(Story_GalleryBrowser.State.Home.class)
@Decl.Child(Point_GalleryBrowser_Home.ToChoiceEditor.class)
@Decl.Child(Point_GalleryBrowser_ChoiceEditor._DefinitionEditor.class)
@Feature.Parent(Feature_Dirndl_ChoiceEditor.class)
public class Point_GalleryBrowser_ChoiceEditor extends Waypoint {
	static final String XPATH_USER_EDITOR = "//definition-editor//choice-editor/edit";

	static final String XPATH_OVERLAY = "//overlay[contains(@class,'choices-editor-multiple')]";

	static final String _REL_EDIT_ENTRY = "//suggesting-node";

	static final String _REL_CHOICE_LARS = "//suggestion[.='lars']";

	@Decl.Child(_DefinitionEditor.ClickSuggestionsArea.class)
	@Decl.Child(_DefinitionEditor.AwaitOverlay.class)
	@Decl.Child(_DefinitionEditor.SendName.class)
	@Decl.Child(_DefinitionEditor.ClickLars.class)
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

		/*
		 * romcom - this is actually a perfect example of concurrent editing
		 * (issues) , since "lars" gets truncated effectively to "l" by
		 * server-side action - see
		 * Point_GalleryBrowser_ChoiceEditor_MutationConflict
		 */
		@Decl.Location.CurrentFocus
		@Decl.Action.UI.Keys("lar")
		static class SendName extends Waypoint {
		}

		@Decl.Location.Xpath({ XPATH_OVERLAY, _REL_CHOICE_LARS })
		@Decl.Action.UI.Click
		static class ClickLars extends Waypoint {
		}
	}
}

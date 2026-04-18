package cc.alcina.framework.servlet.story.component.gallery;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.model.search.Feature_Dirndl_SearchDefinitionEditor;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.gwt.client.story.Waypoints;

/*
 * This tests the SequenceComponent gallery page. Each child resets the initial
 * state
 */
@Feature.Ref(Feature_Dirndl_SearchDefinitionEditor.class)
@Decl.Child(Point_GalleryBrowser_SequenceComponent._TestSearch.class)
public class Point_GalleryBrowser_SequenceComponent extends Waypoint {
	@Decl.Require(Story_GalleryBrowser.State.Home.class)
	@Decl.Child(Point_GalleryBrowser_Home.ToSequenceComponent.class)
	static class _Reset extends Waypoint {
	}

	static final String XPATH_EDITOR = "//sequence-component//edit";

	static final String XPATH_OVERLAY = "//overlay[contains(@class,'choices-editor-multiple')]";

	static final String _REL_CHOICE_MUTATIONS = "//suggestion[contains(.,'mutations')]";

	static final String XPATH_SELECT = "//search-definition-editor//searchable//select";

	static class _DefinitionEditor extends Waypoint {
		@Decl.Location.Xpath(XPATH_EDITOR)
		@Decl.Action.UI.Click
		static class _ClickEditor extends Waypoint {
		}

		@Decl.Location.Xpath({ XPATH_OVERLAY, _REL_CHOICE_MUTATIONS })
		@Decl.Action.UI.Click
		static class _ClickIsMutations extends Waypoint {
		}

		@Decl.Location.Xpath({ XPATH_SELECT })
		@Decl.Action.UI.AwaitPresent
		static class _AwaitSelectFocus extends Waypoint {
		}

		@Decl.Location.CurrentFocus
		@Decl.Action.UI.Keys("t")
		static class _SelectTrue extends Waypoint {
		}
	}

	@Decl.Child(_Reset.class)
	@Decl.Child(_DefinitionEditor._ClickEditor.class)
	@Decl.Child(Waypoints.Wait100.class)
	@Decl.Child(_DefinitionEditor._ClickIsMutations.class)
	@Decl.Child(_DefinitionEditor._AwaitSelectFocus.class)
	@Decl.Child(_DefinitionEditor._SelectTrue.class)
	@Decl.Child(Point_GalleryBrowser_ChoiceEditor.AwaitSuggestingNodeFocus.class)
	@Decl.Child(Waypoints.SendKeyEnter.class)
	static class _TestSearch extends Waypoint {
	}
}

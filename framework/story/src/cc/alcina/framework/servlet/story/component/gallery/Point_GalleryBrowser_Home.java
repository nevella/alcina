package cc.alcina.framework.servlet.story.component.gallery;

import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.story.component.gallery.Point_GalleryBrowser_Home.Cards;

/*
 * This tests the report browser home page
 */
@Decl.Require(Story_GalleryBrowser.State.Home.class)
@Decl.Child(Cards.class)
public class Point_GalleryBrowser_Home extends Waypoint {
	static final String XPATH_CHOICE_EDITOR_LINK = "//a[.='Choice editor']";

	static final String XPATH_CHOICE_MODEL_TITLE = "//choice-editor-gallery//heading[.='Demo Model (contains users collection)']";

	static final String XPATH_SEARCHDEF_EDITOR_LINK = "//a[.='Search definition editor']";

	static final String XPATH_SEARCHDEF_MODEL = "//search-definition-editor-gallery";

	@Decl.Child(ToChoiceEditor.class)
	static class Cards extends Waypoint {
	}

	@Decl.Child(ToChoiceEditor.ClickAreaLink.class)
	@Decl.Child(ToChoiceEditor.AwaitGalleryArea.class)
	static class ToChoiceEditor extends Waypoint {
		@Decl.Location.Xpath(XPATH_CHOICE_EDITOR_LINK)
		@Decl.Action.UI.Click
		static class ClickAreaLink extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_CHOICE_MODEL_TITLE)
		@Decl.Action.UI.AwaitPresent
		static class AwaitGalleryArea extends Waypoint {
		}
	}

	@Decl.Child(ToSearchDefinitionEditor.ClickAreaLink.class)
	@Decl.Child(ToSearchDefinitionEditor.AwaitGalleryArea.class)
	static class ToSearchDefinitionEditor extends Waypoint {
		@Decl.Location.Xpath(XPATH_SEARCHDEF_EDITOR_LINK)
		@Decl.Action.UI.Click
		static class ClickAreaLink extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_SEARCHDEF_MODEL)
		@Decl.Action.UI.AwaitPresent
		static class AwaitGalleryArea extends Waypoint {
		}
	}
}

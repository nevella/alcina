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

	static final String XPATH_MODEL_TITLE = "//choice-editor-gallery//heading[.='Demo Model (contains users collection)']";

	@Decl.Child(ToChoiceEditor.class)
	static class Cards extends Waypoint {
	}

	@Decl.Child(ToChoiceEditor.ClickMultipleSuggestions.class)
	@Decl.Child(ToChoiceEditor.AwaitGalleryDefinitionTitle.class)
	static class ToChoiceEditor extends Waypoint {
		@Decl.Location.Xpath(XPATH_CHOICE_EDITOR_LINK)
		@Decl.Action.UI.Click
		static class ClickMultipleSuggestions extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_MODEL_TITLE)
		@Decl.Action.UI.AwaitPresent
		static class AwaitGalleryDefinitionTitle extends Waypoint {
		}
	}
}

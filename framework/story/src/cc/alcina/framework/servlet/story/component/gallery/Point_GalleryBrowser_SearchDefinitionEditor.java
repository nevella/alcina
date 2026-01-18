package cc.alcina.framework.servlet.story.component.gallery;

import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;

/*
 * This tests the SearchDefinitionEditor gallery page
 */
@Decl.Require(Story_GalleryBrowser.State.Home.class)
@Decl.Child(Point_GalleryBrowser_Home.ToSearchDefinitionEditor.class)
@Decl.Child(Point_GalleryBrowser_SearchDefinitionEditor._DefinitionEditor.class)
public class Point_GalleryBrowser_SearchDefinitionEditor extends Waypoint {
	static final String XPATH_CREATED_FROM_INPUT = "//search-definition-editor//searchable[@criterion-class='CreatedFromCriterion']//input";

	@Decl.Child(_DefinitionEditor.ClickCreatedFrom.class)
	@Decl.Child(_DefinitionEditor.SetCreatedFromText.class)
	static class _DefinitionEditor extends Waypoint {
		@Decl.Location.Xpath(XPATH_CREATED_FROM_INPUT)
		@Decl.Action.UI.Click
		static class ClickCreatedFrom extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_CREATED_FROM_INPUT)
		@Decl.Action.UI.Keys("01-01-2025")
		static class SetCreatedFromText extends Waypoint {
		}
	}
}

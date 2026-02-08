package cc.alcina.framework.servlet.story.component.gallery;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.model.search.Feature_Dirndl_SearchDefinitionEditor;
import cc.alcina.framework.gwt.client.story.SeleniumKeys;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;

/*
 * This tests the SearchDefinitionEditor gallery page
 */
@Decl.Require(Story_GalleryBrowser.State.Home.class)
@Decl.Child(Point_GalleryBrowser_Home.ToSearchDefinitionEditor.class)
@Decl.Child(Point_GalleryBrowser_SearchDefinitionEditor._DefinitionEditor.class)
@Feature.Parent(Feature_Dirndl_SearchDefinitionEditor.class)
public class Point_GalleryBrowser_SearchDefinitionEditor extends Waypoint {
	static final String XPATH_CREATED_FROM_INPUT = "//search-definition-editor//searchable[@criterion-class='CreatedFromCriterion']//input";

	static final String XPATH_CURSOR_TARGET = "//search-definition-editor//cursor-target";

	static final String XPATH_GO_BUTTON_DISABLED = "//search-definition-editor//button[.='Go'][disabled='true']";

	@Decl.Child(_DefinitionEditor.ClickCreatedFrom.class)
	@Decl.Child(_DefinitionEditor.SetCreatedFromText.class)
	@Decl.Child(_DefinitionEditor.EnterOnCreatedFromText.class)
	// @Decl.Child(_DefinitionEditor.TestFocusIsCursorTarget.class)
	// @Decl.Child(_DefinitionEditor.TestGoButtonIsEnabled.class)
	// @Decl.Child(_DefinitionEditor.EnterOnCurrentFocus.class)
	// @Decl.Child(_DefinitionEditor.TestGoButtonIsDisabled.class)
	static class _DefinitionEditor extends Waypoint {
		@Decl.Location.Xpath(XPATH_CREATED_FROM_INPUT)
		@Decl.Action.UI.Click
		static class ClickCreatedFrom extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_CREATED_FROM_INPUT)
		@Decl.Action.UI.Keys("01-01-2025")
		static class SetCreatedFromText extends Waypoint {
		}

		@Feature.Parent(Feature_Dirndl_SearchDefinitionEditor._EnterBehaviour.class)
		@Decl.Location.CurrentFocus
		@Decl.Action.UI.KeyConstant(SeleniumKeys.ENTER)
		static class EnterOnCreatedFromText extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_CURSOR_TARGET)
		@Decl.Action.UI.AwaitSelection
		static class TestFocusIsCursorTarget extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_GO_BUTTON_DISABLED)
		@Decl.Action.UI.AwaitAbsent
		static class TestGoButtonIsEnabled extends Waypoint {
		}

		/*
		 * sends to current focus
		 */
		@Decl.Location.CurrentFocus
		@Decl.Action.UI.KeyConstant(SeleniumKeys.ENTER)
		static class EnterOnCurrentFocus extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_GO_BUTTON_DISABLED)
		@Decl.Action.UI.AwaitPresent
		static class TestGoButtonIsDisabled extends Waypoint {
		}
	}
}

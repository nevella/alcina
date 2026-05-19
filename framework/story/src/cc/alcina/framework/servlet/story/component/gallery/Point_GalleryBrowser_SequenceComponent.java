package cc.alcina.framework.servlet.story.component.gallery;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.dirndl.model.Feature_Dirndl_TableModel;
import cc.alcina.framework.gwt.client.dirndl.model.search.Feature_Dirndl_SearchDefinitionEditor;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.gwt.client.story.Waypoints;

/*
 * This tests the SequenceComponent gallery page. Each child resets the initial
 * state
 */
@Feature.Ref(Feature_Dirndl_SearchDefinitionEditor.class)
@Decl.Child(Point_GalleryBrowser_SequenceComponent.TestSearch.class)
@Decl.Child(Point_GalleryBrowser_SequenceComponent.TestOrderService.class)
@Decl.Child(Point_GalleryBrowser_SequenceComponent.TestFilterService.class)
public class Point_GalleryBrowser_SequenceComponent extends Waypoint {
	@Decl.Require(Story_GalleryBrowser.State.Home.class)
	@Decl.Child(Point_GalleryBrowser_Home.ToSequenceComponent.class)
	static class _Reset extends Waypoint {
	}

	static final String XPATH_EDITOR = "//sequence-component//edit";

	static final String XPATH_OVERLAY = "//overlay[contains(@class,'choices-editor-multiple')]";

	static final String _REL_CHOICE_MUTATIONS = "//suggestion[contains(.,'mutations')]";

	static final String XPATH_SELECT = "//search-definition-editor//searchable//select";

	static final String XPATH_ONE_SEQUENCE_ELEMENT = "//heading[.='Sequence elements [1]']";

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

		@Decl.Location.Xpath(XPATH_ONE_SEQUENCE_ELEMENT)
		@Decl.Action.UI.AwaitPresent
		static class _VerifyOneRow extends Waypoint {
		}
	}

	@Decl.Child(_Reset.class)
	@Decl.Child(_DefinitionEditor._ClickEditor.class)
	@Decl.Child(Waypoints.Wait100.class)
	@Decl.Child(_DefinitionEditor._ClickIsMutations.class)
	@Decl.Child(_DefinitionEditor._AwaitSelectFocus.class)
	@Decl.Child(_DefinitionEditor._SelectTrue.class)
	@Decl.Child(Point_GalleryBrowser_ChoiceEditor.AwaitSuggestingNodeFocus.class)
	@Decl.Child(Waypoints.Wait100.class)
	@Decl.Child(Waypoints.SendKeyEnter.class)
	@Decl.Child(_DefinitionEditor._VerifyOneRow.class)
	static class TestSearch extends Waypoint {
	}

	/**
	 * Test the order service by ordering the sequence, refreshing + verifying
	 * that the order persists
	 */
	@Feature.Ref(Feature_Dirndl_TableModel._OrderService.class)
	@Decl.Child(_Reset.class)
	@Decl.Child(TestOrderService._ClickSort.class)
	@Decl.Child(Waypoints.Wait100.class)
	@Decl.Child(TestOrderService._ClickSort.class)
	@Decl.Child(TestOrderService._VerifyFirstRowIsLastIndex.class)
	// sort-direction
	public static class TestOrderService extends Waypoint {
		static final String XPATH_SORT = "//ch-content/span[.='Index']";

		static final String XPATH_FIRST_ROW_LAST = "//sequence//tbody/tr[1]/td[1]/value[.='230']";
		// sequence

		@Decl.Location.Xpath(XPATH_SORT)
		@Decl.Action.UI.Click
		static class _ClickSort extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_FIRST_ROW_LAST)
		@Decl.Action.UI.AwaitPresent
		static class _VerifyFirstRowIsLastIndex extends Waypoint {
		}
	}

	/**
	 * Test the filter service by filtering + verifying row count
	 */
	@Feature.Ref(Feature_Dirndl_TableModel._FilterService.class)
	@Decl.Child(_Reset.class)
	@Decl.Child(TestFilterService._ClickFilter.class)
	@Decl.Child(TestFilterService._SelectOperator.class)
	@Decl.Child(TestFilterService._EnterValue.class)
	@Decl.Child(TestFilterService._ClickOff.class)
	public static class TestFilterService extends Waypoint {
		static final String XPATH_FILTER = "//ch-content/column-filter";

		static final String XPATH_OPERATOR = "//filter-editor/select";

		static final String XPATH_INPUT = "//filter-editor//input";

		static final String XPATH_OFF = "//left";

		static final String XPATH_FIRST_ROW_LAST = "//sequence//tbody/tr[1]/td[1]/value[.='230']";
		// sequence

		@Decl.Location.Xpath(XPATH_FILTER)
		@Decl.Action.UI.Click
		static class _ClickFilter extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_OPERATOR)
		@Decl.Action.UI.Select.ByText("Equals")
		static class _SelectOperator extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_INPUT)
		@Decl.Action.UI.KeysWithClear("183")
		static class _EnterValue extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_OFF)
		@Decl.Action.UI.Click
		static class _ClickOff extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_FIRST_ROW_LAST)
		@Decl.Action.UI.AwaitPresent
		static class _VerifyFirstRowIsLastIndex extends Waypoint {
		}
	}
}

package cc.alcina.framework.servlet.story.component.traversal;

import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalProcessView_DotBurger;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalProcessView_Header;

@Decl.Feature(Feature_TraversalProcessView_Header.class)
@Decl.Child(Header.Dotburger.class)
class Header extends Waypoint {
	static final String XPATH_DOTBURGER_ICON = "//header/right/dropdown/button[@class='dotburger']";

	static final String XPATH_DOTBURGER_MENU = "//overlay[@class='dotburger dropdown overlay menu']";

	static final String XPATH_DOTBURGER_MENU_DISPLAY_MODE_QUARTER_WIDTH = "//overlay[@class='dotburger dropdown overlay menu']//menu/heading[.='Property display mode']/following-sibling::choices/choice[.='QUARTER_WIDTH']";

	static final String XPATH_DOTBURGER_MENU_DISPLAY_MODE_HALF_WIDTH = "//overlay[@class='dotburger dropdown overlay menu']//menu/heading[.='Property display mode']/following-sibling::choices/choice[.='HALF_WIDTH']";

	@Decl.Doc.HighlightUiNode
	@Decl.Label("Application menu")
	@Decl.Description("The application menu provides access to application"
			+ " settings such as window display and selection ancestry modes")
	@Decl.Feature(Feature_TraversalProcessView_DotBurger.class)
	@Decl.Todo("Add reference links for the modes")
	@Decl.Child(Dotburger.Reset.class)
	@Decl.Child(Dotburger.DocOpen.class)
	@Decl.Child(Dotburger.Open.class)
	@Decl.Child(Dotburger.DisplayMode.class)
	static class Dotburger extends Waypoint {
		static interface State extends Story.State {
			/*
			 * Actually - these aren't candiadates for a 'state' - but that's a
			 * discussion in itself.
			 * 
			 * When to use state/require and when to use child/ensure?
			 * 
			 * Notes to a solution: mostly use the latter, only use the former
			 * if the *whole subtree, without exceptions* requires a state.
			 * Anything that may change within the subtree (and that includes
			 * menu showing/not showing, although that wasn't initially clear)
			 * should use ensure.
			 * 
			 * Possibly with Properties story impl, revert to states (since the
			 * menu elements should be described by the components they affect,
			 * not the menu)
			 */
		//@formatter:off
		static interface MenuShowing extends State {}
		static interface MenuNotShowing extends State {}
		//@formatter:on
		}

		@Decl.Conditional.ExitOkOnFalse(EnsureShowing.TestNotShowing.class)
		@Decl.Child(EnsureShowing.TestNotShowing.class)
		@Decl.Child(EnsureShowing.ClickButton.class)
		@Decl.Child(EnsureShowing.AwaitShowing.class)
		static class EnsureShowing extends Waypoint
				implements Story.State.Provider<State.MenuShowing> {
			@Decl.Location.Xpath(XPATH_DOTBURGER_MENU)
			@Decl.Action.UI.TestAbsent
			static class TestNotShowing extends Waypoint {
			}

			@Decl.Location.Xpath(XPATH_DOTBURGER_ICON)
			@Decl.Action.UI.Click
			static class ClickButton extends Waypoint {
			}

			@Decl.Location.Xpath(XPATH_DOTBURGER_MENU)
			@Decl.Action.UI.TestPresent
			static class TestShowing extends Waypoint
					implements Story.Action.Test {
			}

			@Decl.Location.Xpath(XPATH_DOTBURGER_MENU)
			@Decl.Action.UI.AwaitAbsent
			static class AwaitNotShowing extends Waypoint {
			}

			@Decl.Location.Xpath(XPATH_DOTBURGER_MENU)
			@Decl.Action.UI.AwaitPresent
			static class AwaitShowing extends Waypoint
					implements Story.Action.Test {
			}
		}

		@Decl.Conditional.ExitOkOnFalse(EnsureShowing.TestShowing.class)
		@Decl.Child(EnsureShowing.TestShowing.class)
		@Decl.Child(EnsureShowing.ClickButton.class)
		@Decl.Child(EnsureShowing.AwaitNotShowing.class)
		static class EnsureNotShowing extends Waypoint
				implements Story.State.Provider<State.MenuNotShowing> {
		}

		@Decl.Child(EnsureNotShowing.class)
		static class Reset extends Waypoint {
		}

		@Decl.Doc.HighlightUiNode
		@Decl.Location.Xpath(XPATH_DOTBURGER_ICON)
		@Decl.Label("Displaying the menu")
		@Decl.Description("Click to open the application menu")
		static class DocOpen extends Waypoint {
		}

		/*
		 * The dotburger test is simply 'ensure that the menu showing'
		 */
		@Decl.Child(EnsureShowing.class)
		static class Open extends Waypoint {
		}

		/*
		 * 
		 * This tests the change of DisplayMode from quarterwidth to halfwidth
		 * (by reset/open, which causes a re-render)
		 * 
		 * It doesn't check how that's *rendered* - that's the responsiblity of
		 * the Properties test
		 */
		@Decl.Child(Reset.class)
		@Decl.Child(Open.class)
		@Decl.Child(EnsureDisplayMode_QuarterWidth_Selected.class)
		@Decl.Child(DisplayMode.Click_DisplayMode_HalfWidth.class)
		@Decl.Child(Reset.class)
		@Decl.Child(Open.class)
		@Decl.Child(DisplayMode.Check_DisplayMode_HalfWidth_Selected.class)
		static class DisplayMode extends Waypoint {
			@Decl.Location.Xpath(XPATH_DOTBURGER_MENU_DISPLAY_MODE_HALF_WIDTH)
			@Decl.Action.UI.Click
			static class Click_DisplayMode_HalfWidth extends Waypoint {
			}

			@Decl.Location.Xpath(XPATH_DOTBURGER_MENU_DISPLAY_MODE_HALF_WIDTH)
			@Decl.Action.UI.AwaitAttributePresent("_selected")
			static class Check_DisplayMode_HalfWidth_Selected extends Waypoint {
			}
		}

		@Decl.Child(EnsureDisplayMode_QuarterWidth_Selected.Test_DisplayMode_QuarterWidth_Selected.class)
		@Decl.Child(EnsureDisplayMode_QuarterWidth_Selected.Click_DisplayMode_QuarterWidth.class)
		@Decl.Child(Reset.class)
		@Decl.Child(Open.class)
		@Decl.Child(EnsureDisplayMode_QuarterWidth_Selected.Check_DisplayMode_QuarterWidth_Selected.class)
		static class EnsureDisplayMode_QuarterWidth_Selected extends Waypoint {
			@Decl.Location.Xpath(XPATH_DOTBURGER_MENU_DISPLAY_MODE_QUARTER_WIDTH)
			@Decl.Action.UI.TestAttributePresent("_selected")
			static class Test_DisplayMode_QuarterWidth_Selected
					extends Waypoint {
			}

			@Decl.Location.Xpath(XPATH_DOTBURGER_MENU_DISPLAY_MODE_QUARTER_WIDTH)
			@Decl.Action.UI.Click
			static class Click_DisplayMode_QuarterWidth extends Waypoint {
			}

			@Decl.Location.Xpath(XPATH_DOTBURGER_MENU_DISPLAY_MODE_QUARTER_WIDTH)
			@Decl.Action.UI.AwaitAttributePresent("_selected")
			static class Check_DisplayMode_QuarterWidth_Selected
					extends Waypoint {
			}
		}
	}
}

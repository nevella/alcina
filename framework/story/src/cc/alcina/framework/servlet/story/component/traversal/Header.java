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

	@Decl.Doc.HighlightUiNode
	@Decl.Label("Application menu")
	@Decl.Description("The application menu provides access to application"
			+ " settings such as window display and selection ancestry modes")
	@Decl.Feature(Feature_TraversalProcessView_DotBurger.class)
	@Decl.Todo("Add reference links for the modes")
	@Decl.Child(Dotburger.Reset.class)
	@Decl.Child(Dotburger.DocOpen.class)
	@Decl.Child(Dotburger.Open.class)
	static class Dotburger extends Waypoint {
		static interface State extends Story.State {
		//@formatter:off
		static interface MenuShowing extends State {}
		static interface MenuNotShowing extends State {}
		//@formatter:on
		}

		@Decl.Conditional.ExitOkOnFalse(EnsureShowing.TestNotShowing.class)
		@Decl.Child(EnsureShowing.TestNotShowing.class)
		@Decl.Child(EnsureShowing.ClickButton.class)
		@Decl.Child(EnsureShowing.TestShowing.class)
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
		}

		@Decl.Conditional.ExitOkOnFalse(EnsureShowing.TestShowing.class)
		@Decl.Child(EnsureShowing.TestShowing.class)
		@Decl.Child(EnsureShowing.ClickButton.class)
		@Decl.Child(EnsureShowing.TestNotShowing.class)
		static class EnsureNotShowing extends Waypoint
				implements Story.State.Provider<State.MenuNotShowing> {
		}

		@Decl.Require(State.MenuNotShowing.class)
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
		@Decl.Require(State.MenuShowing.class)
		static class Open extends Waypoint {
		}
	}
}

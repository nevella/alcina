package cc.alcina.framework.servlet.story.component.traversal;

import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalProcessView_DotBurger;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalProcessView_Header;

@Decl.Feature(Feature_TraversalProcessView_Header.class)
class Header extends Waypoint {
	@Decl.Feature(Feature_TraversalProcessView_DotBurger.class)
	static class Dotburger extends Waypoint {
		/*
		 * Declarative types
		 */
		static interface State extends Story.State {
		//@formatter:off
		static interface MenuShowing extends State {}
		static interface MenuNotShowing extends State {}
		//@formatter:on
		}

		@Decl.Child(EnsureShowing.TestShowing.class)
		@Decl.Child(EnsureShowing.ClickButton.class)
		@Decl.Child(EnsureShowing.TestShowing.class)
		static class EnsureShowing extends Waypoint
				implements Story.State.Provider<State.MenuShowing> {
			static class TestShowing extends Waypoint
					implements Story.Action.Test {
			}

			static class ClickButton extends Waypoint {
			}
		}

		/*
		 * The dotburger test is just ensure that the menu is showing
		 */
		@Decl.Require(State.MenuShowing.class)
		static class Test extends Waypoint {
		}
	}
}

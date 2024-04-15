package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Waypoint;

/*
 * Implementations of Story_TraversalProcessView action performers
 */
class Story_TraversalProcessViewImpl {
	/*
	 * Ensures the console is running
	 */
	static class EnsuresConsoleRunning extends Waypoint implements
			Story.State.Provider<Story_TraversalProcessView.State.ConsoleRunning>,
			Story.Action.Code {
		@Override
		public void perform(Context context) {
		}
	}

	/*
	 * Ensures the traversal was performed
	 */
	static class EnsuresCroissanteriaTraversalPerformed extends Waypoint
			implements
			Story.State.Provider<Story_TraversalProcessView.State.CroissanteriaTraversalPerformed> {
	}
}

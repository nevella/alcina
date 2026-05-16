package cc.alcina.framework.servlet.story.component.serverconsole;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.component.console.Feature_ServerConsole;
import cc.alcina.framework.servlet.story.console.Story_Console;

/**
 * The story of the {@link Story_ServerConsole}
 */
@Feature.Ref(Feature_ServerConsole.class)
public class Story_ServerConsole implements Story {
	@Override
	public Point getPoint() {
		return new Top();
	}

	public interface State extends Story.State {
		//@formatter:off
		static interface ServerConsolePageRenders extends State {}
		public interface Loaded extends State {}
		public interface Home extends State {}
        //@formatter:on
	}

	@Decl.Label("ServerConsole UI")
	/*
	 * All points in the story will require these states. They (the states)
	 * could also be represented as a depends chain - i.e.
	 * CroissanteriaTraversalPerformed requires ConsoleRunning (which is true),
	 * this is just a tad clearer
	 */
	@Decl.Require(Story_Console.State.ConsoleConditionalRestart.class)
	@Decl.Require(Story_Console.State.ConsoleRunning.class)
	@Decl.Require(State.ServerConsolePageRenders.class)
	@Decl.Require(State.Loaded.class)
	@Decl.Child(Point_ServerConsole_Home.class)
	@Decl.Child(Point_ServerConsole_Romcom_Sessions.class)
	@Decl.Child(Point_ServerConsole_Romcom_Session.class)
	static class Top extends Waypoint {
	}
}
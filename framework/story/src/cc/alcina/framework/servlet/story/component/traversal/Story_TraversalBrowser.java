package cc.alcina.framework.servlet.story.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalBrowser;
import cc.alcina.framework.servlet.story.console.Story_Console;

/**
 * <p>
 * The story of the {@link Feature_TraversalBrowser}
 * 
 * <p>
 * To traverse/tell it,
 * <code>/g/alcina/subproject/console/sh/launch.alcina.devconsole.sh tell-traversal</code>
 * 
 * <p>
 * To modify the telling context, modify the local class
 * AlcinaStoryTellers.Story_TraversalBrowser_PeerImpl
 */
@Feature.Ref(Feature_TraversalBrowser.class)
public class Story_TraversalBrowser implements Story {
	/*
	 * Declarative types
	 */
	static interface State extends Story.State {
		//@formatter:off
		static interface CroissanteriaTraversalPerformed extends State {}
		static interface TraversalUiLoaded extends State {}
		//@formatter:on
	}

	//
	@Decl.Feature(Feature_TraversalBrowser.class)
	/*
	 * All points in the story will require these states. They (the states)
	 * could also be represented as a depends chain - i.e.
	 * CroissanteriaTraversalPerformed requires ConsoleRunning (which is true),
	 * this is just a tad clearer
	 */
	@Decl.Require(Story_Console.State.ConsoleConditionalRestart.class)
	@Decl.Require(Story_Console.State.ConsoleRunning.class)
	@Decl.Require(State.CroissanteriaTraversalPerformed.class)
	@Decl.Require(State.TraversalUiLoaded.class)
	/*
	 * Children
	 */
	@Decl.Child(_Header.class)
	static class Top extends Waypoint {
	}

	Point top;

	@Override
	public Point getPoint() {
		if (top == null) {
			top = new Top();
		}
		return top;
	}
}

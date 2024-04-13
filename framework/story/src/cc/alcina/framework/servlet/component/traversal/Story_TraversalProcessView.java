package cc.alcina.framework.servlet.component.traversal;

import java.util.List;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Waypoint;

/**
 * The story of the {@link Feature_TraversalProcessView}
 */
@Feature.Ref(Feature_TraversalProcessView.class)
public class Story_TraversalProcessView implements Story {
	/*
	 * Declarative types
	 */
	//@formatter:off
	static interface State extends Story.State{
		static interface ConsoleRunning extends State {}
		static interface CroissanteriaTraversalPerformed extends State {}
	}
	//@formatter:on

	/*
	 * 
	 */
	static class Top extends Waypoint {
		Top() {
			/*
			 * All points in the story will require these states
			 */
			requires = List.of(State.ConsoleRunning.class,
					State.CroissanteriaTraversalPerformed.class);
		}
	}

	/*
	 * Ensures the console is running
	 */
	static class EnsuresConsoleRunning extends Waypoint
			implements Story.State.Provider<State.ConsoleRunning> {
	}

	/*
	 * Ensures the traversal was performed
	 */
	static class EnsuresCroissanteriaTraversalPerformed extends Waypoint
			implements
			Story.State.Provider<State.CroissanteriaTraversalPerformed> {
	}

	public Class<? extends Feature> getFeature() {
		return Feature_TraversalProcessView.class;
	}

	@Override
	public Point getPoint() {
		return new Top();
	}
}

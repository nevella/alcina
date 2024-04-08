package cc.alcina.framework.servlet.component.traversal;

import java.util.List;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.story.Story;

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
	static class Top implements Story.Point {
		/*
		 * All points in the story will require these states
		 */
		public List<Class<? extends Story.State>> getRequires() {
			return List.of(State.ConsoleRunning.class,
					State.CroissanteriaTraversalPerformed.class);
		}

		Top() {
		}
	}

	/*
	 * Ensures the console is running
	 */
	@Story.State.Provides(State.ConsoleRunning.class)
	static class EnsuresConsoleRunning implements Story.Point {
		EnsuresConsoleRunning() {
		}
	}

	public Class<? extends Feature> getFeature() {
		return Feature_TraversalProcessView.class;
	}

	@Override
	public Point getPoint() {
		return new Top();
	}
}

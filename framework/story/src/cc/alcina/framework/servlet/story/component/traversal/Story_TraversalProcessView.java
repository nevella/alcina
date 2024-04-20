package cc.alcina.framework.servlet.story.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalProcessView;

/**
 * The story of the {@link Feature_TraversalProcessView}
 */
@Feature.Ref(Feature_TraversalProcessView.class)
public class Story_TraversalProcessView implements Story {
	/*
	 * Declarative types
	 */
	static interface State extends Story.State {
		//@formatter:off
		static interface ConsoleRunning extends State {}
		static interface CroissanteriaTraversalPerformed extends State {}
		static interface TraversalUiLoaded extends State {}
		//@formatter:on
	}

	//
	@Decl.Feature(Feature_TraversalProcessView.class)
	/*
	 * All points in the story will require these states
	 */
	@Decl.Require(State.ConsoleRunning.class)
	@Decl.Require(State.CroissanteriaTraversalPerformed.class)
	/*
	 * Children
	 */
	@Decl.Child(Header.class)
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

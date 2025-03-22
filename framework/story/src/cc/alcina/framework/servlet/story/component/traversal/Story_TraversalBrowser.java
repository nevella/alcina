package cc.alcina.framework.servlet.story.component.traversal;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalBrowser;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalBrowser_SelectionCards;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalBrowser_SelectionTable;

/**
 * <p>
 * The story of the {@link Feature_TraversalBrowser}, traversing the
 * Croissanteria example traversal
 * 
 * <p>
 * To traverse/tell it,
 * <code>/g/alcina/subproject/console/sh/launch.alcina.devconsole.sh tell-traversal</code>
 * 
 * <p>
 * To modify the telling context, modify the local class
 * AlcinaStoryTellers.Story_TraversalBrowser_PeerImpl
 * <p>
 * TODO: add a Column_filter and Column_sort test once client
 * transactions/alcina croissainteria are implemented
 */
@Feature.Ref(Feature_TraversalBrowser.class)
@Feature.Ref(Feature_TraversalBrowser_SelectionTable.Column_filter.class)
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

	Point top;

	@Override
	public Point getPoint() {
		if (top == null) {
			top = new _Top();
		}
		return top;
	}
}

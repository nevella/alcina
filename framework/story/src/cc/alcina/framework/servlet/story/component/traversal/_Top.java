package cc.alcina.framework.servlet.story.component.traversal;

import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.component.traversal.Feature_TraversalBrowser;
import cc.alcina.framework.servlet.story.component.traversal.Story_TraversalBrowser.State;
import cc.alcina.framework.servlet.story.console.Story_Console;

/**
 * 
 */
@Decl.Feature(Feature_TraversalBrowser.class)
/*
 * All points in the story will require these states. They (the states) could
 * also be represented as a depends chain - i.e. CroissanteriaTraversalPerformed
 * requires ConsoleRunning (which is true), this is just a tad clearer
 */
@Decl.Require(Story_Console.State.ConsoleConditionalRestart.class)
@Decl.Require(Story_Console.State.ConsoleRunning.class)
@Decl.Require(State.CroissanteriaTraversalPerformed.class)
@Decl.Require(State.TraversalUiLoaded.class)
/*
 * Children
 */
@Decl.Child(_Top._Doc.class)
@Decl.Child(_Header.class)
class _Top extends Waypoint {
	@Decl.Label("The Recipe Parser/Traversal Browser")
	@Decl.Description("res:_Top_Doc.md")
	static class _Doc extends Waypoint {
	}
}
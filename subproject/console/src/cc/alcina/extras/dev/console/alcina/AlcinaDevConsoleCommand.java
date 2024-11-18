package cc.alcina.extras.dev.console.alcina;

import cc.alcina.extras.dev.console.DevConsoleCommand;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.story.StoryTeller;
import cc.alcina.framework.gwt.client.story.StoryTellerPeer;
import cc.alcina.framework.servlet.example.traversal.recipe.markup.RecipeMarkupParser;
import cc.alcina.framework.servlet.story.component.traversal.Story_TraversalBrowser;

public abstract class AlcinaDevConsoleCommand extends DevConsoleCommand {
	public static class CmdTestLocal extends AlcinaDevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "l", "local" };
		}

		@Override
		public String getDescription() {
			return "Test local (non-vcs)";
		}

		@Override
		public String getUsage() {
			return "";
		}

		@Override
		public boolean rerunIfMostRecentOnRestart() {
			return true;
		}

		@Override
		public String run(String[] argv) throws Exception {
			CmdExecRunnable cmdExecRunnable = new CmdExecRunnable();
			cmdExecRunnable.console = console;
			return cmdExecRunnable.run(new String[] { "AlcDevLocal" });
		}
	}

	public static class CmdTraverseCroissainteria
			extends AlcinaDevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "croissant" };
		}

		@Override
		public String getDescription() {
			return "Run the croissanteria traversal (for ui testing/demo)";
		}

		@Override
		public String getUsage() {
			return "";
		}

		@Override
		public boolean rerunIfMostRecentOnRestart() {
			return false;
		}

		@Override
		public String run(String[] argv) throws Exception {
			String recipe = Io.read().relativeTo(RecipeMarkupParser.class)
					.resource("croissant-1.xml").asString();
			new RecipeMarkupParser().test(recipe);
			return "parsed";
		}
	}

	public static class CmdTellTraversal extends AlcinaDevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "tell-traversal" };
		}

		@Override
		public String getDescription() {
			return "Tell the traversal story";
		}

		@Override
		public String getUsage() {
			return "";
		}

		@Override
		public boolean rerunIfMostRecentOnRestart() {
			return false;
		}

		@Override
		public String run(String[] argv) throws Exception {
			Story_TraversalBrowser story = new Story_TraversalBrowser();
			StoryTellerPeer peer = new StoryTellerPeer();
			StoryTeller teller = new StoryTeller(peer);
			teller.tell(story);
			return "told";
		}
	}
}

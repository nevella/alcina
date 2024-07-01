package cc.alcina.extras.dev.console.alcina;

import java.util.List;

import cc.alcina.extras.dev.codeservice.CodeService;
import cc.alcina.extras.dev.codeservice.PackagePropertiesGenerator;
import cc.alcina.extras.dev.console.DevConsole;
import cc.alcina.extras.dev.console.DevConsoleCommand;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.story.StoryTeller;
import cc.alcina.framework.gwt.client.story.StoryTellerPeer;
import cc.alcina.framework.servlet.example.traversal.recipe.markup.RecipeMarkupParser;
import cc.alcina.framework.servlet.story.component.traversal.Story_TraversalProcessView;

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

	public static class CmdLaunchCodeService extends AlcinaDevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "codeservice" };
		}

		@Override
		public String getDescription() {
			return "Launch the alcina codeservice";
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
			CodeService codeService = new CodeService();
			// the listpath file might contain - say -
			// /g/alcina/framework/servlet\n/g/alcina/bin
			codeService.sourceAndClassPaths = Io.read()
					.resource("codeserver-paths.local.txt").asList();
			codeService.handlerTypes = List
					.of(PackagePropertiesGenerator.class);
			codeService.blockStartThread = DevConsole.getInstance()
					.isExitAfterCommand();
			codeService.start();
			return "started";
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
			Story_TraversalProcessView story = new Story_TraversalProcessView();
			StoryTellerPeer peer = new StoryTellerPeer();
			StoryTeller teller = new StoryTeller(peer);
			teller.tell(story);
			return "told";
		}
	}
}

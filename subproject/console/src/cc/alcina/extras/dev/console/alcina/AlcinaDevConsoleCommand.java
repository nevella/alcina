package cc.alcina.extras.dev.console.alcina;

import java.io.File;
import java.util.stream.Collectors;

import cc.alcina.extras.dev.console.DevConsoleCommand;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.Io.ReadOp.MapType;
import cc.alcina.framework.gwt.client.story.StoryTeller;
import cc.alcina.framework.gwt.client.story.StoryTellerPeer;
import cc.alcina.framework.servlet.example.traversal.recipe.markup.RecipeMarkupParser;
import cc.alcina.framework.servlet.story.component.gallery.Story_GalleryBrowser;
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

	/**
	 * Open a sass folder in a vs.code window, and start the watcher
	 */
	public static class CmdOpenSass extends AlcinaDevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "sass" };
		}

		@Override
		public String getDescription() {
			return "Open a sass folder in a vs.code window, and start the watcher";
		}

		@Override
		public String getUsage() {
			return "sass [name]";
		}

		@Override
		public boolean rerunIfMostRecentOnRestart() {
			return false;
		}

		@Override
		public String run(String[] argv) throws Exception {
			StringMap sassData = Io.read().resource("sass-paths.properties")
					.asMap(MapType.KEYLINE_VALUELINE);
			String name = argv.length == 0 ? null : argv[0];
			String descKey = name + ".desc";
			String pathKey = name + ".path";
			File scriptFile = new File("/tmp/sass-bash.sh");
			scriptFile.delete();
			if (sassData.containsKey(descKey)) {
				String desc = sassData.get(descKey);
				String path = sassData.get(pathKey);
				logger.info("Launching sass :: {}", desc);
				String cmd = Ax.format(
						"cd '%s' && code sass && ./run-watcher.sh", path, name);
				Io.write().string(cmd).toFile(scriptFile);
			} else {
				logger.warn("sass folder '{}' not found", name);
				logger.warn("available folders: {}", sassData.entrySet()
						.stream().filter(e -> e.getKey().endsWith("desc"))
						.map(e -> Ax.format("%s: %s",
								e.getKey().replace(".desc", ""), e.getValue()))
						.collect(Collectors.joining("\n")));
			}
			return "OK";
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
			StoryTellerPeer peer = Registry.impl(StoryTellerPeer.class,
					story.getClass());
			StoryTeller teller = new StoryTeller(peer);
			teller.tell(story);
			return "told";
		}
	}

	public static class CmdTellGallery extends AlcinaDevConsoleCommand {
		@Override
		public String[] getCommandIds() {
			return new String[] { "tell-gallery" };
		}

		@Override
		public String getDescription() {
			return "Tell the gallery story";
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
			Story_GalleryBrowser story = new Story_GalleryBrowser();
			StoryTellerPeer peer = Registry.impl(StoryTellerPeer.class,
					story.getClass());
			StoryTeller teller = new StoryTeller(peer);
			teller.tell(story);
			return "told";
		}
	}
}

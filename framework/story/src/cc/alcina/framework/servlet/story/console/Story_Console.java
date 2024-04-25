package cc.alcina.framework.servlet.story.console;

import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.story.Story;

// TODO - create a feature (this is support code for all stories which require a
// running dev console)
public class Story_Console {
	public static int port() {
		return Configuration.getInt("port");
	}

	//@formatter:off
	/*
	 * Declarative types
	 */
	public interface State extends Story.State {
		public static interface ConsoleConditionalRestart extends State {}
		public static interface ConsoleNotRunning extends State {}
		public static interface ConsoleRunning extends State {}
	}
	public interface Attribute<T> extends Story.Attribute<T> {
		public static interface ConsolePort extends Attribute<Integer> {}
		public static interface ConsoleShouldRestart extends Attribute<Boolean> {}
	}
	//@formatter:on
}

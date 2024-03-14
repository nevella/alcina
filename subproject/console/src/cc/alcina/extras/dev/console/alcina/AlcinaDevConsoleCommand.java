package cc.alcina.extras.dev.console.alcina;

import cc.alcina.extras.dev.console.DevConsoleCommand;

public abstract class AlcinaDevConsoleCommand
		extends DevConsoleCommand<AlcinaDevConsole> {
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
}

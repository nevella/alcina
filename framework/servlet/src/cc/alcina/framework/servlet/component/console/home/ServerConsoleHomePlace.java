package cc.alcina.framework.servlet.component.console.home;

import cc.alcina.framework.servlet.component.console.ServerConsolePlace;

/**
 * 
 * 
 */
public class ServerConsoleHomePlace extends ServerConsolePlace {
	@Override
	public ServerConsoleHomePlace copy() {
		return (ServerConsoleHomePlace) super.copy();
	}

	public static class Tokenizer
			extends ServerConsolePlace.Tokenizer<ServerConsoleHomePlace> {
	}

	@Override
	public String getDescription() {
		return "ServerConsoles Home";
	}
}

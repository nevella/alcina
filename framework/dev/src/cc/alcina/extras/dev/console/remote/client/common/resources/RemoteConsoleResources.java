package cc.alcina.extras.dev.console.remote.client.common.resources;

import com.google.gwt.resources.client.ClientBundle;

import cc.alcina.framework.gwt.client.gen.SimpleCssResource;

public interface RemoteConsoleResources extends ClientBundle {
	@Source("res/fonts.css")
	public SimpleCssResource fontStyles();

	@Source("res/remote-console-styles.css")
	public SimpleCssResource remoteConsoleStyles();

	@Source("res/remote-console-theme.css")
	public SimpleCssResource remoteConsoleThemeStyles();
}

package cc.alcina.framework.servlet.component.console;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;

@ReflectiveSerializer.Checks(ignore = false)
@TypedProperties
public class ServerConsoleSettings extends Bindable.Fields {
	PackageProperties._ServerConsoleSettings.InstanceProperties properties() {
		return PackageProperties.serverConsoleSettings.instance(this);
	}

	public static ServerConsoleSettings get() {
		return ServerConsoleBrowser.Ui.get().settings;
	}

	public boolean compactServerConsoleUi = true;
}

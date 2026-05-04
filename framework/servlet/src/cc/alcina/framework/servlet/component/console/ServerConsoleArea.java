package cc.alcina.framework.servlet.component.console;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@TypedProperties
@Directed(tag = "server-console-area")
class ServerConsoleArea extends Model.Fields {
	PackageProperties._ServerConsoleArea.InstanceProperties properties() {
		return PackageProperties.serverConsoleArea.instance(this);
	}

	@Directed(className = "server-console")
	ServerConsoleContents contents;

	ServerConsolePage page;

	@Binding(type = Type.PROPERTY)
	boolean compactServerConsoleUi;

	ServerConsoleArea(ServerConsolePage page) {
		this.page = page;
		from(page.ui.subtypeProperties().place())
				.map(ServerConsoleContents::forPlace)
				.to(properties().contents()).oneWay();
		from(ServerConsoleSettings.get().properties().compactServerConsoleUi())
				.to(properties().compactServerConsoleUi()).oneWay();
	}
}

package cc.alcina.framework.dev.console.remote.client.common.logic;

import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.dev.console.remote.protocol.RemoteConsoleStartupModel;

public class RemoteConsoleModels {
	public static final Topic<RemoteConsoleStartupModel> topicStartupModelLoaded = Topic
			.create();

	private RemoteConsoleStartupModel startupModel;

	public RemoteConsoleStartupModel getStartupModel() {
		return this.startupModel;
	}

	public void setStartupModel(RemoteConsoleStartupModel startupModel) {
		this.startupModel = startupModel;
		topicStartupModelLoaded.publish(startupModel);
	}
}

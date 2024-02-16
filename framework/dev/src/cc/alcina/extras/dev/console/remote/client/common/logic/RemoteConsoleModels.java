package cc.alcina.extras.dev.console.remote.client.common.logic;

import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleStartupModel;
import cc.alcina.framework.common.client.util.Topic;

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

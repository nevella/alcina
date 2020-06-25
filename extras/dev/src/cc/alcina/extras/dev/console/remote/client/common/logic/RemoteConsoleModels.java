package cc.alcina.extras.dev.console.remote.client.common.logic;

import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleStartupModel;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;

public class RemoteConsoleModels {
    public static final String TOPIC_REMOTE_CONSOLE_STARTUP_MODEL_LOADED = RemoteConsoleModels.class
            .getName() + "." + "TOPIC_REMOTE_CONSOLE_STARTUP_MODEL_LOADED";

    public static Topic<RemoteConsoleStartupModel> topicStartupModelLoaded() {
        return Topic.global(TOPIC_REMOTE_CONSOLE_STARTUP_MODEL_LOADED);
    }

    private RemoteConsoleStartupModel startupModel;

    public RemoteConsoleStartupModel getStartupModel() {
        return this.startupModel;
    }

    public void setStartupModel(RemoteConsoleStartupModel startupModel) {
        this.startupModel = startupModel;
        topicStartupModelLoaded().publish(startupModel);
    }
}

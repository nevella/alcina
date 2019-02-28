package cc.alcina.extras.dev.console.remote.protocol;

import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@Bean
public class RemoteConsoleRequest extends BaseBindable {
    private String commandString;

    private String completionString;

    private RemoteConsoleRequestType type;

    public String getCommandString() {
        return this.commandString;
    }

    public String getCompletionString() {
        return this.completionString;
    }

    public RemoteConsoleRequestType getType() {
        return this.type;
    }

    public void setCommandString(String commandString) {
        this.commandString = commandString;
    }

    public void setCompletionString(String completionString) {
        this.completionString = completionString;
    }

    public void setType(RemoteConsoleRequestType type) {
        this.type = type;
    }

    @ClientInstantiable
    public enum RemoteConsoleRequestType {
        STARTUP, GET_RECORDS, COMPLETE, DO_COMMAND, ARROW_UP, ARROW_DOWN
    }
}

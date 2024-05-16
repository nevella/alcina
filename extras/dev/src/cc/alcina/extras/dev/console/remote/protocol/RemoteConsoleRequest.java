package cc.alcina.extras.dev.console.remote.protocol;

import java.util.Date;

import com.google.common.base.Preconditions;
import com.google.gwt.core.shared.GWT;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.DateStyle;

public class RemoteConsoleRequest extends Bindable {
	private transient static String generatedClientInstanceUid;

	public static RemoteConsoleRequest create() {
		Preconditions.checkArgument(GWT.isClient());
		if (generatedClientInstanceUid == null) {
			generatedClientInstanceUid = Ax.format("%s__%s",
					DateStyle.TIMESTAMP_HUMAN.format(new Date()),
					Math.random());
		}
		RemoteConsoleRequest consoleRequest = new RemoteConsoleRequest();
		consoleRequest.clientInstanceUid = generatedClientInstanceUid;
		return consoleRequest;
	}

	private String commandString;

	private String completionString;

	private String clientInstanceUid;

	private RemoteConsoleRequestType type;

	public RemoteConsoleRequest() {
	}

	public String getClientInstanceUid() {
		return this.clientInstanceUid;
	}

	public String getCommandString() {
		return this.commandString;
	}

	public String getCompletionString() {
		return this.completionString;
	}

	public RemoteConsoleRequestType getType() {
		return this.type;
	}

	public void setClientInstanceUid(String clientInstanceUid) {
		this.clientInstanceUid = clientInstanceUid;
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

	@Reflected
	public enum RemoteConsoleRequestType {
		STARTUP, GET_RECORDS, COMPLETE, DO_COMMAND, ARROW_UP, ARROW_DOWN
	}
}

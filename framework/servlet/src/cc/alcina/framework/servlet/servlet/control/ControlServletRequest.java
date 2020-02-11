package cc.alcina.framework.servlet.servlet.control;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class ControlServletRequest {
	private ControlServletRequestCommand command;

	private ControlServletModes modes;

	private boolean json;

	public ControlServletRequestCommand getCommand() {
		return this.command;
	}

	public ControlServletModes getModes() {
		return this.modes;
	}

	public boolean isJson() {
		return this.json;
	}

	public void setCommand(ControlServletRequestCommand command) {
		this.command = command;
	}

	public void setJson(boolean json) {
		this.json = json;
	}

	public void setModes(ControlServletModes modes) {
		this.modes = modes;
	}

	@Override
	public String toString() {
		return Ax.format("\tcmd:\t%s\n" + "\tstates:\t%s\n",
				CommonUtils.nullSafeToString(command),
				CommonUtils.nullSafeToString(modes));
	}
}

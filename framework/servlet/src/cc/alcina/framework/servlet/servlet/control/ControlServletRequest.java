package cc.alcina.framework.servlet.servlet.control;

public class ControlServletRequest {
	private ControlServletRequestCommand command;

	private boolean json;

	public ControlServletRequestCommand getCommand() {
		return this.command;
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
}

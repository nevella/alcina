package cc.alcina.framework.servlet.servlet.control;

import java.util.Date;

public class ControlServletStatus {
	private Date startupTime;

	private String appName;

	private ControlServletStates states;

	public String getAppName() {
		return this.appName;
	}

	public Date getStartupTime() {
		return this.startupTime;
	}

	public ControlServletStates getStates() {
		return this.states;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setStartupTime(Date startupTime) {
		this.startupTime = startupTime;
	}

	public void setStates(ControlServletStates states) {
		this.states = states;
	}
}

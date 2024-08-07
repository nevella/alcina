package cc.alcina.framework.servlet.servlet.control;

import java.util.Date;

import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration(ControlServletState.class)
public class ControlServletState {
	private Date startupTime;

	private String appName;

	private String writerHost;

	private String apiKey;

	public ControlServletState() {
	}

	public String getApiKey() {
		return this.apiKey;
	}

	public String getAppName() {
		return this.appName;
	}

	public Date getStartupTime() {
		return this.startupTime;
	}

	public String getWriterHost() {
		return this.writerHost;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setStartupTime(Date startupTime) {
		this.startupTime = startupTime;
	}

	public void setWriterHost(String writerHost) {
		this.writerHost = writerHost;
	}
}

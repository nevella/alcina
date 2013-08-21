package cc.alcina.framework.servlet.servlet.control;

import java.util.Date;

import cc.alcina.framework.common.client.util.CommonUtils;

public class ControlServletState {
	private Date startupTime;

	private String appName;

	private String writerHost;

	private ControlServletModes modes = new ControlServletModes();

	private String apiKey;

	public String getApiKey() {
		return this.apiKey;
	}

	public String getAppName() {
		return this.appName;
	}

	public ControlServletModes getModes() {
		return this.modes;
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

	public void setModes(ControlServletModes modes) {
		this.modes = modes;
	}

	public void setStartupTime(Date startupTime) {
		this.startupTime = startupTime;
	}

	public void setWriterHost(String writerHost) {
		this.writerHost = writerHost;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("\tstartup:\t%s\n" + "\tapp name:\t%s\n"
				+ "\tapi key:\t%s\n" + "\tstates:\t\t%s\n",
				CommonUtils.nullSafeToString(startupTime),
				CommonUtils.nullSafeToString(appName),
				CommonUtils.nullSafeToString(apiKey),
				CommonUtils.nullSafeToString(modes));
	}

	public static ControlServletState standaloneModes() {
		ControlServletState state = new ControlServletState();
		state.setModes(ControlServletModes.standaloneModes());
		return state;
	}

	public static ControlServletState memberModes() {
		ControlServletState state = new ControlServletState();
		state.setModes(ControlServletModes.memberModes());
		return state;
	}
}

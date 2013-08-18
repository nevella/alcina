package cc.alcina.framework.servlet.servlet.control;

import java.util.Date;

import cc.alcina.framework.common.client.util.CommonUtils;

public class ControlServletStatus {
	private Date startupTime;

	private String appName;
	
	private String writerUrl;

	private ControlServletModes modes=new ControlServletModes();

	public String getAppName() {
		return this.appName;
	}

	public Date getStartupTime() {
		return this.startupTime;
	}

	public ControlServletModes getModes() {
		return this.modes;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setStartupTime(Date startupTime) {
		this.startupTime = startupTime;
	}

	public void setModes(ControlServletModes modes) {
		this.modes = modes;
	}
	@Override
	public String toString() {
		return CommonUtils.formatJ("\tstartup:\t%s\n" + "\tapp name:\t%s\n"+ "\tstates:\t%s\n",
				CommonUtils.nullSafeToString(startupTime),
				CommonUtils.nullSafeToString(appName),
				CommonUtils.nullSafeToString(modes));
	}

	public String getWriterUrl() {
		return this.writerUrl;
	}

	public void setWriterUrl(String writerUrl) {
		this.writerUrl = writerUrl;
	}
}

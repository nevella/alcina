package cc.alcina.framework.servlet.servlet.control;

import cc.alcina.framework.common.client.util.CommonUtils;

public class ControlServletResponse {
	private ControlServletRequest request;

	private ControlServletState status;

	private String message;

	public ControlServletRequest getRequest() {
		return this.request;
	}

	public void setRequest(ControlServletRequest request) {
		this.request = request;
	}

	public ControlServletState getStatus() {
		return this.status;
	}

	public void setStatus(ControlServletState status) {
		this.status = status;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ(
				"ControlServletResponse:\n================\n"
						+ "request:\n%s\n\nstatus:\n%s\n\nmessage:\n%s\n\n",
				CommonUtils.nullSafeToString(request),
				CommonUtils.nullSafeToString(status),
				CommonUtils.nullSafeToString(message));
	}
}

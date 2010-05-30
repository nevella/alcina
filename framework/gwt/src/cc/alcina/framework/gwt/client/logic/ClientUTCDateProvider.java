package cc.alcina.framework.gwt.client.logic;

import java.util.Date;

import cc.alcina.framework.common.client.util.CurrentUtcDateProvider;

public class ClientUTCDateProvider implements CurrentUtcDateProvider {
	@SuppressWarnings("deprecation")
	public Date currentUtcDate() {
		Date d = new Date();
		return new Date(d.getTime() + d.getTimezoneOffset() * 60 * 1000);
	}
}

package cc.alcina.framework.common.client.log;

import cc.alcina.framework.common.client.log.TaggedLogger.TaggedLoggerHandler;
import cc.alcina.framework.common.client.util.CommonUtils;

public class SysoutHandler implements TaggedLoggerHandler {
	private String prefix;

	public SysoutHandler(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void log(String message) {
		if (prefix == null) {
			System.out.println(message);
		} else {
			System.out.println(CommonUtils.formatJ("%s: %s", prefix, message));
		}
	}
}

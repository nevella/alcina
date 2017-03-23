package cc.alcina.framework.common.client.log;

import cc.alcina.framework.common.client.log.TaggedLogger.TaggedLoggerHandler;
import cc.alcina.framework.common.client.util.CommonUtils;

public class StringBuilderHandler implements TaggedLoggerHandler {
	private StringBuilder stringBuilder = new StringBuilder();

	@Override
	public String toString() {
		return stringBuilder.toString();
	}

	@Override
	public void log(String message) {
		stringBuilder.append(message);
		stringBuilder.append("\n");
	}
}

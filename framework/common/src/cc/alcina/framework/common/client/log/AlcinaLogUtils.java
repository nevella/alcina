package cc.alcina.framework.common.client.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;

public class AlcinaLogUtils {
	public static Logger getMetricLogger(Class clazz) {
		return getTaggedLogger(clazz, "metric");
	}

	public static Logger getTaggedLogger(Class clazz, String tag) {
		return LoggerFactory
				.getLogger(Ax.format("%s.__%s", clazz.getName(), tag));
	}
}

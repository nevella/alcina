package cc.alcina.framework.common.client.logic.reflection.jvm;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.Io;

public class FsLogger {
	public void write(String log) {
		try {
			Io.write().string(log).toPath("/tmp/gwt-fs-log.txt");
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}

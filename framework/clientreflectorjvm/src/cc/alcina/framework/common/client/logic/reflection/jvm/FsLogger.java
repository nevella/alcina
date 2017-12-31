package cc.alcina.framework.common.client.logic.reflection.jvm;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.ResourceUtilities;

public class FsLogger {
	public void write(String log) {
		try {
			ResourceUtilities.writeStringToFile(log, "/tmp/gwt-fs-log.txt");
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}

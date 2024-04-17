package cc.alcina.framework.entity.util;

import java.io.FileOutputStream;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.util.LineCallback;

public class FileAppenderCallback implements LineCallback {
	private String prompt;

	private String path;

	public FileAppenderCallback(String prompt, String path) {
		this.path = path;
		this.prompt = prompt;
	}

	@Override
	public void accept(String value) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			Io.write().string(Ax.format("%s%s\n", prompt, value)).toStream(fos);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
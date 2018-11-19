package cc.alcina.framework.entity.util;

import java.io.FileOutputStream;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.entity.ResourceUtilities;

public class FileAppenderCallback implements Callback<String> {
	private String prompt;

	private String path;

	public FileAppenderCallback(String prompt, String path) {
		this.path = path;
		this.prompt = prompt;
	}

	@Override
	public void apply(String value) {
		try {
			FileOutputStream fos = new FileOutputStream(path, true);
			ResourceUtilities.writeStringToOutputStream(
					Ax.format("%s%s\n", prompt, value), fos);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
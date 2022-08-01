package cc.alcina.extras.dev.console;

import java.io.File;
import java.util.Base64;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;

public class DevUtils {
	public static void fileToBase64(String path) {
		try {
			byte[] bytes = ResourceUtilities
					.readFileToByteArray(new File(path));
			String encoded = Base64.getEncoder().encodeToString(bytes);
			DevConsole.getInstance().setClipboardContents(encoded);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public static void fileToDataUrl(String path) {
		fileToDataUrl(path, "image/png");
	}

	public static void fileToDataUrl(String path, String mimeType) {
		try {
			byte[] bytes = ResourceUtilities
					.readFileToByteArray(new File(path));
			String encoded = Base64.getEncoder().encodeToString(bytes);
			String url = Ax.format("data:%s;base64,%s", mimeType, encoded);
			DevConsole.getInstance().setClipboardContents(url);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}

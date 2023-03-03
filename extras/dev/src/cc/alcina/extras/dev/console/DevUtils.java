package cc.alcina.extras.dev.console;

import java.util.Base64;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;

public class DevUtils {
	public static void fileToBase64(String path) {
		try {
			byte[] bytes = Io.read().path(path).asBytes();
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
			byte[] bytes = Io.read().path(path).asBytes();
			String encoded = Base64.getEncoder().encodeToString(bytes);
			String url = Ax.format("data:%s;base64,%s", mimeType, encoded);
			DevConsole.getInstance().setClipboardContents(url);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public static void stringToBase64(String string) {
		String encoded = Base64.getEncoder().encodeToString(string.getBytes());
		DevConsole.getInstance().setClipboardContents(encoded);
	}
}

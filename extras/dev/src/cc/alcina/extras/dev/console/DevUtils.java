package cc.alcina.extras.dev.console;

import java.util.Base64;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;

public class DevUtils {
	public static String fileToBase64(String path) {
		try {
			byte[] bytes = Io.read().path(path).asBytes();
			String encoded = Base64.getEncoder().encodeToString(bytes);
			DevConsole.getInstance().setClipboardContents(encoded);
			return encoded;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public static String fileToDataUrl(String path) {
		return fileToDataUrl(path, "image/png");
	}

	public static String fileToDataUrl(String path, String mimeType) {
		try {
			byte[] bytes = Io.read().path(path).asBytes();
			String encoded = Base64.getEncoder().encodeToString(bytes);
			String url = Ax.format("data:%s;base64,%s", mimeType, encoded);
			DevConsole.getInstance().setClipboardContents(url);
			return url;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public static String stringToBase64(String string) {
		String encoded = Base64.getEncoder().encodeToString(string.getBytes());
		DevConsole.getInstance().setClipboardContents(encoded);
		return string;
	}
}

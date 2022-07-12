package cc.alcina.extras.dev.console;

import java.io.File;
import java.util.Base64;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;

public class DevUtils {
	public static void fileToDataUrl(String path) {
		try {
			byte[] bytes = ResourceUtilities
					.readFileToByteArray(new File(path));
			String encoded = Base64.getEncoder().encodeToString(bytes);
			String url = Ax.format("data:image/png;base64,%s", encoded);
			DevConsole.getInstance().setClipboardContents(url);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}

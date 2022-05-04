package cc.alcina.framework.common.client.util.dev;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public class DevmodeUtils {
	public static void log(String content) {
		try {
			Path path = Paths.get("/tmp/devmode.txt");
			byte[] strToBytes = content.getBytes(StandardCharsets.UTF_8);
			Files.write(path, strToBytes);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}

package cc.alcina.extras.dev.console;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import cc.alcina.extras.dev.console.DevConsoleCommand.CmdExecRunnable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

@RegistryLocation(registryPoint = DevConsoleRunnable.class)
public abstract class DevConsoleRunnable extends AbstractTaskPerformer {
	public abstract String[] tagStrings();

	public DevConsole console;

	public CmdExecRunnable command;

	public static final String CONTEXT_ACTION_RESULT = CmdExecRunnable.class
			.getName() + ".CONTEXT_ACTION_RESULT";

	protected String writeTempFile(Class clazz, String extension, String content)
			throws IOException {
		File dir = console.devHelper.getDevFolder();
		String outPath = String.format("%s/%s.%s", dir.getPath(),
				clazz.getSimpleName(), extension);
		ResourceUtilities.writeStringToFile(content, new File(outPath));
		return outPath;
	}

	protected String writeTempFileBytes(Class clazz, String extension,
			String content) throws IOException {
		File dir = console.devHelper.getDevFolder();
		String outPath = String.format("%s/%s.%s", dir.getPath(),
				clazz.getSimpleName(), extension);
		;
		ResourceUtilities
				.writeStreamToStream(
						new ByteArrayInputStream(Base64Utils
								.fromBase64(content)), new FileOutputStream(
								outPath));
		return outPath;
	}

	public boolean canUseProductionConn() {
		return false;
	}
}
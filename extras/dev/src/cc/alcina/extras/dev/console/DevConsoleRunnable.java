package cc.alcina.extras.dev.console;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cc.alcina.extras.dev.console.DevConsoleCommand.CmdExecRunnable;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

@RegistryLocation(registryPoint = DevConsoleRunnable.class)
@TypeSerialization(flatSerializable = false)
public abstract class DevConsoleRunnable extends AbstractTaskPerformer {
	public static final String CONTEXT_ACTION_RESULT = CmdExecRunnable.class
			.getName() + ".CONTEXT_ACTION_RESULT";

	public static DevConsole console;

	public CmdExecRunnable command;

	public String[] argv;

	public boolean canUseProductionConn() {
		return false;
	}

	public boolean requiresDomainStore() {
		return true;
	}

	public boolean rerunIfMostRecentOnRestart() {
		return true;
	}

	@Override
	public void run() {
		try {
			run0();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void runAsSubcommand(DevConsoleRunnable parentRunnable) {
		command = parentRunnable.command;
		argv = new String[0];// don't pass through - this is all devvy
		run();
	}

	public void runFromCommand(DevConsoleCommand command, String[] argv) {
		this.argv = argv;
		run();
	}

	// typed a lot - so alias
	public void sub(DevConsoleRunnable parentRunnable) {
		runAsSubcommand(parentRunnable);
	}

	public abstract String[] tagStrings();

	@Override
	public DevConsoleRunnable withValue(String value) {
		return (DevConsoleRunnable) super.withValue(value);
	}

	protected String writeTempFile(Class clazz, String extension,
			String content) throws IOException {
		File dir = console.devHelper.getDevFolder();
		String outPath = String.format("%s/%s.%s", dir.getPath(),
				clazz.getSimpleName(), extension);
		ResourceUtilities.writeStringToFile(content, new File(outPath));
		return outPath;
	}

	protected String writeTempFileBytes(Class clazz, String extension,
			String content) throws IOException {
		if (!Base64Utils.isBase64(content)) {
			return writeTempFile(clazz, extension, content);
		}
		File dir = console.devHelper.getDevFolder();
		String outPath = String.format("%s/%s.%s", dir.getPath(),
				clazz.getSimpleName(), extension);
		ResourceUtilities.writeStreamToStream(
				new ByteArrayInputStream(Base64Utils.fromBase64(content)),
				new FileOutputStream(outPath));
		return outPath;
	}

	protected String writeTempFileFs(Class clazz, String extension,
			File content) throws IOException {
		File dir = console.devHelper.getDevFolder();
		String outPath = String.format("%s/%s.%s", dir.getPath(),
				clazz.getSimpleName(), extension);
		;
		ResourceUtilities.writeStreamToStream(new FileInputStream(content),
				new FileOutputStream(outPath));
		return outPath;
	}
}
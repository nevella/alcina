package cc.alcina.extras.dev.console;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cc.alcina.extras.dev.console.DevConsoleCommand.CmdExecRunnable;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.servlet.actionhandlers.AbstractTaskPerformer;

@RegistryLocation(registryPoint = DevConsoleRunnable.class)
public abstract class DevConsoleRunnable extends AbstractTaskPerformer {
	public static final String CONTEXT_ACTION_RESULT = CmdExecRunnable.class
			.getName() + ".CONTEXT_ACTION_RESULT";

	public DevConsole console;

	public CmdExecRunnable command;

	public String[] argv;

	public AbstractTaskPerformer asSubTask(DevConsoleRunnable parentRunnable) {
		console = parentRunnable.console;
		command = parentRunnable.command;
		argv = new String[0];// don't pass through - this is all devvy
		return super.asSubTask(parentRunnable);
	}

	public boolean canUseProductionConn() {
		return false;
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

	public void runAsSubtask(DevConsoleRunnable parentRunnable) {
		console = parentRunnable.console;
		command = parentRunnable.command;
		argv = new String[0];// don't pass through - this is all devvy
		super.asSubTask(parentRunnable).run();
	}

	public void runFromCommand(DevConsoleCommand command, String[] argv) {
		console = command.console;
		this.argv = argv;
		actionLogger = command.logger;
		run();
	}

	public abstract String[] tagStrings();

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
package cc.alcina.extras.dev.console;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cc.alcina.extras.dev.console.DevConsoleCommand.CmdExecRunnable;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.servlet.schedule.PerformerTask;

@TypeSerialization(flatSerializable = false)
@Registration(DevConsoleRunnable.class)
public abstract class DevConsoleRunnable extends PerformerTask {
	public static final String CONTEXT_ACTION_RESULT = CmdExecRunnable.class
			.getName() + ".CONTEXT_ACTION_RESULT";

	public static DevConsole console;

	public CmdExecRunnable command;

	public String[] argv;

	public boolean canUseProductionConn() {
		return false;
	}

	protected void logJobResultFiles() {
		Ax.out("Job result files:\n/tmp/log/log.xml\n  /tmp/log/log.html");
	}

	public boolean requiresDomainStore() {
		return true;
	}

	public boolean rerunIfMostRecentOnRestart() {
		return true;
	}

	public void runAsSubcommand(DevConsoleRunnable parentRunnable) {
		command = parentRunnable.command;
		argv = new String[0];// don't pass through - this is all devvy
		runFromCommand(command, argv);
	}

	public void runFromCommand(DevConsoleCommand command, String[] argv) {
		this.argv = argv;
		try {
			DevConsole.instance.currentRunnables.push(this);
			run();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		} finally {
			DevConsole.instance.currentRunnables.pop();
		}
	}

	// typed a lot - so alias
	public void sub() {
		sub(DevConsole.instance.currentRunnables.peek());
	}

	// typed a lot - so alias
	public void sub(DevConsoleRunnable parentRunnable) {
		runAsSubcommand(parentRunnable);
	}

	public abstract String[] tagStrings();

	public static String writeTempFile(Class clazz, String extension,
			String content) throws IOException {
		File dir = console.getDevHelper().getDevFolder();
		String outPath = String.format("%s/%s.%s", dir.getPath(),
				clazz.getSimpleName(), extension);
		Io.write().string(content).toFile(new File(outPath));
		return outPath;
	}

	protected String writeTempFileBytes(Class clazz, String extension,
			String content) throws IOException {
		if (!Base64Utils.isBase64(content)) {
			return writeTempFile(clazz, extension, content);
		}
		File dir = console.getDevHelper().getDevFolder();
		String outPath = String.format("%s/%s.%s", dir.getPath(),
				clazz.getSimpleName(), extension);
		Io.Streams.copy(
				new ByteArrayInputStream(Base64Utils.fromBase64(content)),
				new FileOutputStream(outPath));
		return outPath;
	}

	protected String writeTempFileFs(Class clazz, String extension,
			File content) throws IOException {
		File dir = console.getDevHelper().getDevFolder();
		String outPath = String.format("%s/%s.%s", dir.getPath(),
				clazz.getSimpleName(), extension);
		;
		Io.Streams.copy(new FileInputStream(content),
				new FileOutputStream(outPath));
		return outPath;
	}
}
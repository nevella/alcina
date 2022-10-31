package cc.alcina.framework.entity.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.NamedThreadFactory;

public class Shell {
	public static String exec(String script, Object... args) {
		try {
			String command = Ax.format(script, args);
			LoggerFactory.getLogger(Shell.class).info(command);
			Shell shell = new Shell();
			shell.logToStdOut = false;
			Output output = shell.runBashScript(command, false)
					.throwOnException();
			if (Ax.notBlank(output.error)) {
				Ax.err(output.error);
			}
			return output.output;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void receiveStream(StreamBuffer streamBuffer) {
		Pool.get().threadPool.execute(streamBuffer);
	}

	public String ERROR_MARKER = "**>";

	public String OUTPUT_MARKER = "";

	public int timeoutMs = 0;

	private Timer timer;

	private Process process;

	protected boolean timedOut;

	public boolean logToStdOut = true;

	public String logToFile = null;

	private StreamBuffer errorBuffer;

	private StreamBuffer outputBuffer;

	private boolean terminated;

	public Process getProcess() {
		return this.process;
	}

	public boolean isTerminated() {
		return this.terminated;
	}

	public File launchBashScript(String script) throws Exception {
		File tmp = File.createTempFile("shell", getScriptExtension());
		tmp.deleteOnExit();
		ResourceUtilities.writeStringToFile(script, tmp);
		launchProcess(new String[] { "/bin/bash", tmp.getPath() },
				s -> s.length(), s -> s.length());
		return tmp;
	}

	public void launchProcess(String[] cmdAndArgs,
			Callback<String> outputCallback, Callback<String> errorCallback)
			throws IOException {
		if (cmdAndArgs[0].equals("/bin/bash") && isWindows()) {
			List<String> rewrite = Arrays.asList(cmdAndArgs).stream()
					.collect(Collectors.toList());
			// just run as .bat
			rewrite.remove(0);
			cmdAndArgs = (String[]) rewrite.toArray(new String[rewrite.size()]);
		}
		if (logToStdOut) {
			System.out.format("launching process: %s\n",
					CommonUtils.join(cmdAndArgs, " "));
		}
		ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
		process = pb.start();
		errorBuffer = new StreamBuffer(process.getErrorStream(), errorCallback);
		outputBuffer = new StreamBuffer(process.getInputStream(),
				outputCallback);
		receiveStream(errorBuffer);
		receiveStream(outputBuffer);
	}

	public Shell noLogging() {
		logToStdOut = false;
		return this;
	}

	public Output runBashScript(String script) throws Exception {
		return runBashScript(script, false);
	}

	public Output runBashScript(String script, boolean logCmd)
			throws Exception {
		File tmp = File.createTempFile("shell", getScriptExtension());
		tmp.deleteOnExit();
		ResourceUtilities.writeStringToFile(script, tmp);
		Output output = runShell(tmp.getPath(), "/bin/bash");
		tmp.delete();
		return output;
	}

	public Output runBashScriptAndThrow(String script) throws Exception {
		Output tuple = runBashScript(script, true);
		if (tuple.failed()) {
			throw new Exception(tuple.error);
		} else {
			return tuple;
		}
	}

	public void runBashScriptNoThrow(String cmd, Object... args) {
		try {
			runBashScript(Ax.format(cmd, args));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public Output runProcessCatchOutputAndWait(String... cmdAndArgs)
			throws Exception {
		return runProcessCatchOutputAndWaitPrompt("", cmdAndArgs);
	}

	public Output runProcessCatchOutputAndWait(String[] cmdAndArgs,
			Callback<String> outputCallback, Callback<String> errorCallback)
			throws Exception {
		launchProcess(cmdAndArgs, outputCallback, errorCallback);
		return waitFor();
	}

	public Output runProcessCatchOutputAndWaitPrompt(String prompt,
			String... cmdAndArgs) throws Exception {
		if (logToStdOut) {
			return runProcessCatchOutputAndWait(cmdAndArgs,
					new TabbedSysoutCallback(prompt + OUTPUT_MARKER),
					new TabbedSysoutCallback(prompt + ERROR_MARKER));
		} else if (logToFile != null) {
			return runProcessCatchOutputAndWait(cmdAndArgs,
					new FileAppenderCallback(prompt + OUTPUT_MARKER, logToFile),
					new FileAppenderCallback(prompt + ERROR_MARKER, logToFile));
		} else {
			return runProcessCatchOutputAndWait(cmdAndArgs, s -> s.length(),
					s -> s.length());
		}
	}

	public Output runShell(String argString) throws Exception {
		return runShell(argString, "/bin/sh");
	}

	public Output runShell(String argString, String shellCmd) throws Exception {
		List<String> args = new ArrayList<String>();
		args.add(shellCmd);
		args.addAll(Arrays.asList(argString.split(" ")));
		String[] argv = (String[]) args.toArray(new String[args.size()]);
		return runProcessCatchOutputAndWait(argv);
	}

	public void terminateProcess() {
		this.terminated = true;
		process.destroy();
		try {
			if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
				process.destroyForcibly();
				process.waitFor(500, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException e) {
		}
	}

	public Output waitFor() throws InterruptedException {
		if (timeoutMs != 0) {
			timer = new Timer();
			TimerTask killProcessTask = new TimerTask() {
				@Override
				public void run() {
					System.out.println("Killing process (timeout)");
					timedOut = true;
					process.destroy();
					timer.cancel();
				}
			};
			timer.schedule(killProcessTask, timeoutMs);
		}
		process.waitFor();
		outputBuffer.waitFor();
		errorBuffer.waitFor();
		if (timer != null) {
			timer.cancel();
		}
		return new Output(outputBuffer.getStreamResult(),
				errorBuffer.getStreamResult(), timedOut, process.exitValue(),
				Ax.isBlank(logToFile) ? null
						: ResourceUtilities.read(logToFile));
	}

	private String getScriptExtension() {
		return isWindows() ? ".bat" : ".sh";
	}

	private boolean isWindows() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.indexOf("win") >= 0;
	}

	public static class Output {
		public String output;

		public String error;

		public boolean timedOut;

		public int exitValue;

		public String logFileContents;

		public Output(String output, String error, boolean timedOut,
				int exitValue, String logFileContents) {
			this.output = output;
			this.error = error;
			this.timedOut = timedOut;
			this.exitValue = exitValue;
			this.logFileContents = logFileContents;
		}

		public boolean failed() {
			return exitValue != 0;
		}

		public Output throwOnException() {
			if (failed()) {
				throw Ax.runtimeException(this.toString());
			}
			return this;
		}

		@Override
		public String toString() {
			FormatBuilder fb = new FormatBuilder();
			fb.line("Process output");
			fb.line("==============");
			fb.line("exit code:%s", exitValue);
			fb.line("output:\n%s\n", output);
			fb.line("error:\n%s\n", error);
			return fb.toString();
		}
	}

	@Registration.Singleton
	public static class Pool {
		static Shell.Pool get() {
			return Registry.impl(Shell.Pool.class);
		}

		private ExecutorService threadPool;

		public Pool() {
			this.threadPool = Executors
					.newCachedThreadPool(new NamedThreadFactory("shell-io"));
		}
	}

	public static class RsyncCommand {
		public static Builder builder() {
			return new Builder();
		}

		private boolean keepPermissions;

		private String from;

		private String to;

		private boolean test;

		private boolean update = true;

		private boolean delete = false;

		private String toHost;

		String sshOptions = "";

		private RsyncCommand(Builder builder) {
			this.keepPermissions = builder.keepPermissions;
			this.from = builder.from;
			this.to = builder.to;
			this.test = builder.test;
			this.update = builder.update;
			this.delete = builder.delete;
			this.toHost = builder.toHost;
		}

		public void sync() throws Exception {
			Preconditions.checkNotNull(from);
			Preconditions.checkNotNull(to);
			if (toHost != null) {
				String sshConfigKey = "sshCommand." + toHost;
				String hostConfigKey = "host." + toHost;
				if (Configuration.has(RsyncCommand.class, sshConfigKey)) {
					sshOptions = Configuration.get(RsyncCommand.class,
							sshConfigKey);
				}
				if (Configuration.has(RsyncCommand.class, hostConfigKey)) {
					toHost = Configuration.get(RsyncCommand.class,
							hostConfigKey);
				}
				to = Ax.format("root@%s:%s", toHost, to);
			}
			String flags = "-avz";
			if (keepPermissions) {
				if (update) {
					flags = "-rlptDvzu --size-only";
					// checksum better albeit slower
					flags = "-rlptDvzu --checksum";
				} else {
					flags = "-rlptDvz  --size-only";
					flags = "-rlptDvz  --checksum";
				}
			}
			if (delete) {
				flags += " --delete";
			}
			if (test) {
				flags += " --dry-run --itemize-changes";
			}
			if (Ax.notBlank(sshOptions)) {
				flags += " " + sshOptions;
			}
			Ax.out("Syncing:\n\t%s ->\n\t%s\n", from, to);
			String script = Ax.format("rsync %s %s %s;", flags, from, to);
			new Shell().runBashScript(script).throwOnException();
		}

		public static final class Builder {
			private boolean keepPermissions;

			private String from;

			private String to;

			private boolean test;

			private boolean update;

			private boolean delete = true;

			private String toHost;

			private Builder() {
			}

			public RsyncCommand build() {
				return new RsyncCommand(this);
			}

			public Builder withDelete(boolean delete) {
				this.delete = delete;
				return this;
			}

			public Builder withFrom(String from) {
				this.from = from;
				return this;
			}

			public Builder withKeepPermissions(boolean keepPermissions) {
				this.keepPermissions = keepPermissions;
				return this;
			}

			public Builder withTest(boolean test) {
				this.test = test;
				return this;
			}

			public Builder withTo(String to) {
				this.to = to;
				return this;
			}

			public Builder withToHost(String toHost) {
				this.toHost = toHost;
				return this;
			}

			public Builder withUpdate(boolean update) {
				this.update = update;
				return this;
			}
		}
	}

	public static class SshCommand {
		public static Builder builder() {
			return new Builder();
		}

		private String command;

		private String sshOptions = "";

		private String host;

		private SshCommand(Builder builder) {
			command = builder.command;
			sshOptions = builder.options;
			host = builder.host;
		}

		public void exec() throws Exception {
			Preconditions.checkNotNull(command);
			Preconditions.checkNotNull(host);
			String sshConfigKey = "sshCommand." + host;
			String hostConfigKey = "host." + host;
			if (Configuration.has(SshCommand.class, sshConfigKey)) {
				sshOptions = Configuration.get(SshCommand.class, sshConfigKey);
			}
			if (Configuration.has(SshCommand.class, hostConfigKey)) {
				host = Configuration.get(SshCommand.class, hostConfigKey);
			}
			Ax.out("Exec:\t%s :: \t%s", host, command);
			String script = Ax.format("ssh %s %s %s;", sshOptions, host,
					command);
			new Shell().runBashScript(script).throwOnException();
		}

		public static final class Builder {
			private String command;

			private String host;

			private String options;

			private Builder() {
			}

			public SshCommand build() {
				return new SshCommand(this);
			}

			public Builder withCommand(String command) {
				this.command = command;
				return this;
			}

			public Builder withHost(String host) {
				this.host = host;
				return this;
			}

			public Builder withOptions(String options) {
				this.options = options;
				return this;
			}
		}
	}
}

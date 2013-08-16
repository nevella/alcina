package cc.alcina.framework.entity.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class ShellWrapper {
	public String ERROR_MARKER = "**";

	public String OUTPUT_MARKER = "";

	public int timeoutMs = 0;

	private Timer timer;

	private Process proc;

	protected boolean timedOut;
	

	public void runBashScript(String script) throws Exception {
		File tmp = File.createTempFile("shell", ".sh");
		ResourceUtilities.writeStringToFile(script, tmp);
		runShell(tmp.getPath(), "/bin/bash");
	}

	public ShellOutputTuple runProcessCatchOutputAndWait(String... cmdAndArgs)
			throws Exception {
		return runProcessCatchOutputAndWait(cmdAndArgs, true);
	}

	public ShellOutputTuple runProcessCatchOutputAndWait(String[] cmdAndArgs,
			boolean sysout) throws Exception {
		System.out.format("launching process: %s\n",
				CommonUtils.join(cmdAndArgs, " "));
		ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
		proc = pb.start();
		StreamBuffer errorGobbler = new StreamBuffer(proc.getErrorStream(),
				ERROR_MARKER, sysout);
		StreamBuffer outputGobbler = new StreamBuffer(proc.getInputStream(),
				OUTPUT_MARKER, sysout);
		errorGobbler.start();
		outputGobbler.start();
		if (timeoutMs != 0) {
			timer = new Timer();
			TimerTask killProcessTask = new TimerTask() {
				@Override
				public void run() {
					System.out.println("Killing process (timeout)");
					timedOut=true;
					proc.destroy();
				}
			};
			timer.schedule(killProcessTask, timeoutMs);
		}
		proc.waitFor();
		outputGobbler.waitFor();
		errorGobbler.waitFor();
		if (timer != null) {
			timer.cancel();
		}
		return new ShellOutputTuple(outputGobbler.getStreamResult(),
				errorGobbler.getStreamResult(),timedOut);
	}

	public ShellOutputTuple runShell(String argString) throws Exception {
		return runShell(argString, "/bin/sh");
	}

	public ShellOutputTuple runShell(String argString, String shellCmd)
			throws Exception {
		List<String> args = new ArrayList<String>();
		args.add(shellCmd);
		args.addAll(Arrays.asList(argString.split(" ")));
		String[] argv = (String[]) args.toArray(new String[args.size()]);
		return runProcessCatchOutputAndWait(argv, true);
	}

	public static class ShellOutputTuple {
		public String output;

		public String error;
		
		public boolean timedOut;

		public ShellOutputTuple(String output, String error, boolean timedOut) {
			this.output = output;
			this.error = error;
			this.timedOut = timedOut;
		}
	}
}

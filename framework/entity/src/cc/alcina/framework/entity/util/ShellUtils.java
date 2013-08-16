package cc.alcina.framework.entity.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class ShellUtils {
	public static String ERROR_MARKER="**";
	public static String OUTPUT_MARKER="";
	public static void runBashScript(String script) throws Exception {
		File tmp = File.createTempFile("shell", ".sh");
		ResourceUtilities.writeStringToFile(script, tmp);
		runShell(tmp.getPath(),"/bin/bash");
	}

	public static ShellOutputTuple runProcessCatchOutputAndWait(
			String[] cmdAndArgs) throws Exception {
		return runProcessCatchOutputAndWait(cmdAndArgs, true);
	}

	public static ShellOutputTuple runProcessCatchOutputAndWait(
			String[] cmdAndArgs, boolean sysout) throws Exception {
		System.out.format("launching process: %s\n",
				CommonUtils.join(cmdAndArgs, " "));
		ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
		Process proc = pb.start();
		StreamBuffer errorGobbler = new StreamBuffer(proc.getErrorStream(),
				ERROR_MARKER, sysout);
		StreamBuffer outputGobbler = new StreamBuffer(proc.getInputStream(),
				OUTPUT_MARKER, sysout);
		errorGobbler.start();
		outputGobbler.start();
		proc.waitFor();
		outputGobbler.waitFor();
		errorGobbler.waitFor();
		return new ShellOutputTuple(outputGobbler.getStreamResult(),
				errorGobbler.getStreamResult());
	}
	public static ShellOutputTuple runShell(String argString) throws Exception {
		return runShell(argString,"/bin/sh");
	}
	public static ShellOutputTuple runShell(String argString, String shellCmd) throws Exception {
		List<String> args = new ArrayList<String>();
		args.add(shellCmd);
		args.addAll(Arrays.asList(argString.split(" ")));
		String[] argv = (String[]) args.toArray(new String[args.size()]);
		return runProcessCatchOutputAndWait(argv, true);
	}

	public static class ShellOutputTuple {
		public String output;

		public String error;

		public ShellOutputTuple(String output, String error) {
			this.output = output;
			this.error = error;
		}
	}
}

package cc.alcina.framework.entity.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class ShellWrapper {
    public String ERROR_MARKER = "**>";

    public String OUTPUT_MARKER = "";

    public int timeoutMs = 0;

    private Timer timer;

    private Process process;

    protected boolean timedOut;

    public boolean logToStdOut = true;

    public String logToFile = null;

    private StreamBuffer errorGobbler;

    private StreamBuffer outputGobbler;

    private boolean terminated;

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

    public ShellWrapper noLogging() {
        logToStdOut = false;
        return this;
    }

    public ShellOutputTuple runBashScript(String script) throws Exception {
        return runBashScript(script, false);
    }

    public ShellOutputTuple runBashScript(String script, boolean logCmd)
            throws Exception {
        File tmp = File.createTempFile("shell", getScriptExtension());
        tmp.deleteOnExit();
        ResourceUtilities.writeStringToFile(script, tmp);
        ShellOutputTuple outputTuple = runShell(tmp.getPath(), "/bin/bash");
        tmp.delete();
        return outputTuple;
    }

    public ShellOutputTuple runBashScriptAndThrow(String script)
            throws Exception {
        ShellOutputTuple tuple = runBashScript(script, true);
        if (tuple.failed()) {
            throw new Exception(tuple.error);
        } else {
            return tuple;
        }
    }

    public ShellOutputTuple runProcessCatchOutputAndWait(String... cmdAndArgs)
            throws Exception {
        return runProcessCatchOutputAndWaitPrompt("", cmdAndArgs);
    }

    public ShellOutputTuple runProcessCatchOutputAndWait(String[] cmdAndArgs,
            Callback<String> outputCallback, Callback<String> errorCallback)
            throws Exception {
        launchProcess(cmdAndArgs, outputCallback, errorCallback);
        return waitFor();
    }

    public ShellOutputTuple runProcessCatchOutputAndWaitPrompt(String prompt,
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

    public ShellOutputTuple runShell(String argString) throws Exception {
        return runShell(argString, "/bin/sh");
    }

    public ShellOutputTuple runShell(String argString, String shellCmd)
            throws Exception {
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

    public ShellOutputTuple waitFor() throws InterruptedException {
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
        outputGobbler.waitFor();
        errorGobbler.waitFor();
        if (timer != null) {
            timer.cancel();
        }
        return new ShellOutputTuple(outputGobbler.getStreamResult(),
                errorGobbler.getStreamResult(), timedOut, process.exitValue());
    }

    private String getScriptExtension() {
        return isWindows() ? ".bat" : ".sh";
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.indexOf("win") >= 0;
    }

    protected void launchProcess(String[] cmdAndArgs,
            Callback<String> outputCallback, Callback<String> errorCallback)
            throws IOException {
        if (cmdAndArgs[0].equals("/bin/bash") && isWindows()) {
            List<String> rewrite = Arrays.asList(cmdAndArgs).stream()
                    .collect(Collectors.toList());
            rewrite.remove(0);// just run as .bat
            cmdAndArgs = (String[]) rewrite.toArray(new String[rewrite.size()]);
        }
        if (logToStdOut) {
            System.out.format("launching process: %s\n",
                    CommonUtils.join(cmdAndArgs, " "));
        }
        ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
        process = pb.start();
        errorGobbler = new StreamBuffer(process.getErrorStream(),
                errorCallback);
        outputGobbler = new StreamBuffer(process.getInputStream(),
                outputCallback);
        errorGobbler.start();
        outputGobbler.start();
    }

    public static class ShellOutputTuple {
        public String output;

        public String error;

        public boolean timedOut;

        public int exitValue;

        public ShellOutputTuple(String output, String error, boolean timedOut,
                int exitValue) {
            this.output = output;
            this.error = error;
            this.timedOut = timedOut;
            this.exitValue = exitValue;
        }

        public boolean failed() {
            return exitValue != 0;
        }

        public void throwOnException() {
            if (failed()) {
                throw Ax.runtimeException("ShellOutputTuple exit code %s\n%s",
                        exitValue, error);
            }
        }
    }
}

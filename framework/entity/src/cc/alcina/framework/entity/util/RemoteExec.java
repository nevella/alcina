package cc.alcina.framework.entity.util;

import java.io.File;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.util.Shell.Output;

public class RemoteExec {
	private String targetFolder;

	private String command;

	private RemoteConnection remoteConnection;

	public Output exec() throws Exception {
		if (isRemote()) {
			syncTargetFolder(true);
		}
		File logFile = File.createTempFile("remote", ".txt");
		Shell shell = new Shell();
		shell.logToStdOut = false;
		shell.logToFile = logFile.getPath();
		Output output = exec(command, shell);
		if (isRemote()) {
			syncTargetFolder(false);
		}
		return output;
	}

	public RemoteExec withCommand(String command) {
		this.command = command;
		return this;
	}

	public RemoteExec withRemoteConnection(RemoteConnection remoteConnection) {
		this.remoteConnection = remoteConnection;
		return this;
	}

	public RemoteExec withTargetFolder(String targetFolder) {
		this.targetFolder = targetFolder;
		return this;
	}

	private Output exec(String cmd) throws Exception {
		return exec(cmd, new Shell());
	}

	private Output exec(String cmd, Shell shell) throws Exception {
		String script = cmd;
		if (isRemote()) {
			script = Ax.format(
					"ssh -i %s -o StrictHostKeyChecking=no -p %s -t %s@%s 'PATH=$PATH:/usr/local/bin && %s'",
					remoteConnection.sshPrivateKey, remoteConnection.sshPort,
					remoteConnection.sshUser, remoteConnection.hostName, cmd);
		}
		Ax.out(script);
		Output output = shell.runBashScript(script);
		if (isThrowOnException()) {
			output.throwOnException();
		}
		return output;
	}

	private boolean throwOnException = true;

	public boolean isThrowOnException() {
		return this.throwOnException;
	}

	public void setThrowOnException(boolean throwOnException) {
		this.throwOnException = throwOnException;
	}
	public RemoteExec withThrowOnException(boolean throwOnException) {
		this.throwOnException = throwOnException;
		return this;
	}

	private boolean isRemote() {
		return remoteConnection != null;
	}

	/*
	 * Write to host, then copy to container
	 */
	private void syncTargetFolder(boolean fromToTo) throws Exception {
		if (fromToTo) {
			exec(Ax.format("mkdir -p %s", targetFolder));
		}
		String local = targetFolder + "/";
		String remote = Ax.format("%s@%s:%s/", remoteConnection.sshUser,
				remoteConnection.hostName, targetFolder);
		String from = fromToTo ? local : remote;
		String to = fromToTo ? remote : local;
		String rsync = Ax.format(
				"rsync -avz --delete --rsh \"/usr/bin/ssh -i %s -o StrictHostKeychecking=no -p %s \""
						+ " %s %s",
				remoteConnection.sshPrivateKey, remoteConnection.sshPort, from,
				to);
		Ax.out(rsync);
		new Shell().runBashScript(rsync);
		if (!fromToTo) {
			// TODO - check it's tmp
			// exec(Ax.format("rm -rf %s", targetFolder));
		}
	}

	public static class RemoteConnection {
		String hostName;

		String sshUser;

		String sshPrivateKey;

		int sshPort;

		public RemoteConnection(String hostName, int sshPort, String sshUser,
				String sshPrivateKey) {
			this.hostName = hostName;
			this.sshPort = sshPort;
			this.sshUser = sshUser;
			this.sshPrivateKey = sshPrivateKey;
		}
	}
}

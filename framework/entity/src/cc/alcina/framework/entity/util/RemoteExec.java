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
		Output output = exec(command, shell, true);
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
		return exec(cmd, new Shell(), false);
	}

	private Output exec(String cmd, Shell shell, boolean runInContainer)
			throws Exception {
		String script = cmd;
		if (isRemote()) {
			if (runInContainer) {
				script = Ax.format(
						"ssh -i %s -o StrictHostKeyChecking=no -t %s@%s 'PATH=$PATH:/usr/local/bin &&"
								+ " docker exec %s /bin/bash -c '\"'\"' %s'\"'\"' '",
						remoteConnection.sshPrivateKey,
						remoteConnection.sshUser,
						remoteConnection.dockerHostName,
						remoteConnection.containerName, cmd);
			} else {
				script = Ax.format(
						"ssh -i %s -o StrictHostKeyChecking=no -t %s@%s 'PATH=$PATH:/usr/local/bin && %s'",
						remoteConnection.sshPrivateKey,
						remoteConnection.sshUser,
						remoteConnection.dockerHostName, cmd);
			}
		}
		Ax.out(script);
		Output output = shell.runBashScript(script);
		output.throwOnException();
		return output;
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
				remoteConnection.dockerHostName, targetFolder);
		String from = fromToTo ? local : remote;
		String to = fromToTo ? remote : local;
		if (!fromToTo) {
			exec(Ax.format("docker cp  %s:%s %s/..",
					remoteConnection.containerName, targetFolder,
					targetFolder));
		}
		String rsync = Ax.format(
				"rsync -avz --delete --rsh \"/usr/bin/ssh -i %s -o StrictHostKeychecking=no -p 22\""
						+ " %s %s",
				remoteConnection.sshPrivateKey, from, to);
		Ax.out(rsync);
		new Shell().runBashScript(rsync);
		if (fromToTo) {
			exec(Ax.format("docker cp %s %s:%s", targetFolder,
					remoteConnection.containerName, targetFolder));
		}
		if (!fromToTo) {
			// TODO - check it's tmp
			// exec(Ax.format("rm -rf %s", targetFolder));
		}
	}

	public static class RemoteConnection {
		String containerName;

		String dockerHostName;

		String sshUser;

		String sshPrivateKey;

		public RemoteConnection(String containerName, String dockerHostName,
				String sshUser, String sshPrivateKey) {
			this.containerName = containerName;
			this.dockerHostName = dockerHostName;
			this.sshUser = sshUser;
			this.sshPrivateKey = sshPrivateKey;
		}
	}
}

package cc.alcina.extras.dev.console;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.entity.util.Shell.Output;
import cc.alcina.framework.servlet.servlet.control.ControlServlet;

public class DevConsoleCommandsDeploy {
	public static String getControlServletPath() {
		return Configuration.get("targetContainerControlServletPath");
	}

	public static String invokeRemoteTask(Task task) {
		return invokeRemoteTask(task, true);
	}

	public static String invokeRemoteTask(Task task, boolean wait) {
		return invokeRemoteTask(task,
				ControlServlet.TaskExecutionType.defaultForWait(wait));
	}

	public static String invokeRemoteTask(Task task,
			ControlServlet.TaskExecutionType executionType) {
		String targetContainerControlServletPath = getControlServletPath();
		String targetContainerControlServletKey = Configuration
				.get("targetContainerControlServletKey");
		Ax.out("\n****************\n****************\n** Executing **\n****************\n****************\n\n");
		String response = null;
		if (Configuration.is("executeLocal")) {
			response = null;
			Job job = task.perform();
			response = Ax.blankTo(job.getLog(),
					Ax.format("Job %s - complete", job));
		} else {
			response = ControlServlet.invokeTask(task,
					targetContainerControlServletPath,
					targetContainerControlServletKey, executionType);
		}
		return response;
	}

	// normally call from a task, rather than directly
	public static class CmdDeploy extends DevConsoleCommand {
		@Override
		public boolean clsBeforeRun() {
			return true;
		}

		private void execRemote(String cmd) throws Exception {
			String targetContainerName = Configuration
					.get(DevConsoleCommandsDeploy.class, "targetContainerName");
			String targetDockerHostName = Configuration.get(
					DevConsoleCommandsDeploy.class, "targetDockerHostName");
			String targetDockerHostSshUser = Configuration.get(
					DevConsoleCommandsDeploy.class, "targetDockerHostSshUser");
			String targetDockerHostSshPrivateKey = Configuration.get(
					DevConsoleCommandsDeploy.class,
					"targetDockerHostSshPrivateKey");
			String script = Ax.format(
					"ssh -i %s -o StrictHostKeyChecking=no -t %s@%s '%s'",
					targetDockerHostSshPrivateKey, targetDockerHostSshUser,
					targetDockerHostName, cmd);
			Ax.out(script);
			Output output = new Shell().runBashScript(script);
			output.throwOnException();
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "deploy.cmd" };
		}

		@Override
		public String getDescription() {
			return "deploy a tmp package to a server";
		}

		@Override
		public String getUsage() {
			return "deploy <servlet package>";
		}

		@Override
		public String run(String[] argv) throws Exception {
			String servletPackage = argv[0];
			String servletPackageBase = Configuration
					.get(DevConsoleCommandsDeploy.class, "servletPackageBase");
			String targetContainerName = Configuration
					.get(DevConsoleCommandsDeploy.class, "targetContainerName");
			String targetDockerHostName = Configuration.get(
					DevConsoleCommandsDeploy.class, "targetDockerHostName");
			String targetDockerHostSshUser = Configuration.get(
					DevConsoleCommandsDeploy.class, "targetDockerHostSshUser");
			String targetDockerHostSshPrivateKey = Configuration.get(
					DevConsoleCommandsDeploy.class,
					"targetDockerHostSshPrivateKey");
			String targetContainerExplodedDeployment = Configuration.get(
					DevConsoleCommandsDeploy.class,
					"targetContainerExplodedDeployment");
			String packagePath = servletPackage.replace(".", "/");
			String fromPath = Ax.format("%s/%s", servletPackageBase,
					packagePath);
			String toPath = Ax.format("/tmp/dev/deploy/%s", packagePath);
			execRemote(Ax.format("mkdir -p %s", toPath));
			String rsync = Ax.format(
					"rsync -avz --rsh \"/usr/bin/ssh -i %s -o StrictHostKeychecking=no -p 22\""
							+ " %s/ %s@%s:%s/",
					targetDockerHostSshPrivateKey, fromPath,
					targetDockerHostSshUser, targetDockerHostName, toPath);
			Ax.out(rsync);
			new Shell().runBashScript(rsync);
			String deployPackagePath = Ax.format(
					"/opt/jboss/wildfly/standalone/deployments/%s/WEB-INF/classes/%s",
					targetContainerExplodedDeployment,
					packagePath.replaceFirst("(.+)/.+", "$1"));
			execRemote(Ax.format(
					"PATH=$PATH:/usr/local/bin && docker exec %s /bin/mkdir -p %s "
							+ " && docker cp %s %s:%s"
							+ " && docker exec -u 0 %s chown -R 1000:1000 %s &&  docker exec -u 0 %s chmod -R 777 %s",
					targetContainerName, deployPackagePath, toPath,
					targetContainerName, deployPackagePath, targetContainerName,
					deployPackagePath, targetContainerName, deployPackagePath));
			return "ok";
		}
	}
}

package cc.alcina.extras.dev.console;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.ShellWrapper;
import cc.alcina.framework.entity.util.ShellWrapper.ShellOutputTuple;

public class DevConsoleCommandsDeploy {
	public static class CmdDeploy extends DevConsoleCommand {
		@Override
		public boolean clsBeforeRun() {
			return true;
		}

		@Override
		public String[] getCommandIds() {
			return new String[] { "deploy" };
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
			String servletPackageBase = ResourceUtilities
					.get(DevConsoleCommandsDeploy.class, "servletPackageBase");
			String targetContainerName = ResourceUtilities
					.get(DevConsoleCommandsDeploy.class, "targetContainerName");
			String targetDockerHostName = ResourceUtilities.get(
					DevConsoleCommandsDeploy.class, "targetDockerHostName");
			String targetDockerHostSshUser = ResourceUtilities.get(
					DevConsoleCommandsDeploy.class, "targetDockerHostSshUser");
			String targetDockerHostSshPrivateKey = ResourceUtilities.get(
					DevConsoleCommandsDeploy.class,
					"targetDockerHostSshPrivateKey");
			String targetContainerExplodedDeployment = ResourceUtilities.get(
					DevConsoleCommandsDeploy.class,
					"targetContainerExplodedDeployment");
			String packagePath = servletPackage.replace(".", "/");
			String fromPath = Ax.format("%s/%s", servletPackageBase,
					packagePath);
			String toPath = Ax.format("/tmp/dev/deploy/%s", packagePath);
			exec(Ax.format("mkdir -p %s", toPath));
			String rsync = Ax.format(
					"rsync -avz --rsh \"/usr/bin/ssh -i %s -o StrictHostKeychecking=no -p 22\""
							+ " %s/ %s@%s:%s/",
					targetDockerHostSshPrivateKey, fromPath,
					targetDockerHostSshUser, targetDockerHostName, toPath);
			Ax.out(rsync);
			new ShellWrapper().runBashScript(rsync);
			String deployPackagePath = Ax.format(
					"/opt/jboss/wildfly/standalone/deployments/%s/WEB-INF/classes/%s",
					targetContainerExplodedDeployment,
					packagePath.replaceFirst("(.+)/.+", "$1"));
			exec(Ax.format(
					"PATH=$PATH:/usr/local/bin && docker exec %s /bin/mkdir -p %s "
							+ " && docker cp %s %s:%s"
							+ " && docker exec -u 0 %s chown -R 1000:1000 %s &&  docker exec -u 0 %s chmod -R 777 %s",
					targetContainerName, deployPackagePath, toPath,
					targetContainerName, deployPackagePath, targetContainerName,
					deployPackagePath, targetContainerName, deployPackagePath));
			return "ok";
		}

		private void exec(String cmd) throws Exception {
			String targetContainerName = ResourceUtilities
					.get(DevConsoleCommandsDeploy.class, "targetContainerName");
			String targetDockerHostName = ResourceUtilities.get(
					DevConsoleCommandsDeploy.class, "targetDockerHostName");
			String targetDockerHostSshUser = ResourceUtilities.get(
					DevConsoleCommandsDeploy.class, "targetDockerHostSshUser");
			String targetDockerHostSshPrivateKey = ResourceUtilities.get(
					DevConsoleCommandsDeploy.class,
					"targetDockerHostSshPrivateKey");
			String script = Ax.format(
					"ssh -i %s -o StrictHostKeyChecking=no -t %s@%s '%s'",
					targetDockerHostSshPrivateKey, targetDockerHostSshUser,
					targetDockerHostName, cmd);
			Ax.out(script);
			ShellOutputTuple outputTuple = new ShellWrapper()
					.runBashScript(script);
			outputTuple.throwOnException();
		}
	}
}

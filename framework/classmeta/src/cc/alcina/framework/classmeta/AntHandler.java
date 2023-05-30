package cc.alcina.framework.classmeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.JaxbUtils;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.entity.util.Shell.Output;
import cc.alcina.framework.gwt.client.util.EventCollator;

/*
 * Locking - only have one system-wide compile job run at once - use the file
 * "/tmp/classmeta-anthandler.lock" to lock externally
 */
public class AntHandler extends AbstractHandler {
	static Logger logger = LoggerFactory.getLogger(AntHandler.class);

	static String lockPath = "/tmp/classmeta-anthandler.lock";

	CachingAntModel model;

	public AntHandler() {
		String modelXml = Io.read().relativeTo(ClassMetaServer.class)
				.resource("schema/antModel.xml").asString();
		model = JaxbUtils.xmlDeserialize(CachingAntModel.class, modelXml);
	}

	@Override
	public synchronized void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String cmd = request.getParameter("cmd");
		String cwd = request.getParameter("cwd");
		CachingAntTask task = new CachingAntTask(cmd, cwd);
		model.runningTask = task;
		model.updateTasks(this);
		response.setContentType("text/plain");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write(task.returnCmd);
		baseRequest.setHandled(true);
	}

	@XmlRootElement
	static class CachingAntModel {
		public transient CachingAntTask runningTask;

		public transient CachingAntTask currentTask;

		public List<CachingAntProject> projects = new ArrayList<>();

		private transient CachingAntListener currentListener;

		private transient AntHandler antHandler;

		private Exception lastBuildException;

		public void runBuildTargets() {
			synchronized (antHandler) {
				while (true) {
					if (!new File(lockPath).exists()) {
						break;
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				List<CachingAntProject> resolved = new ArrayList<>(
						currentListener.pendingBuilds);
				currentListener.pendingBuilds.stream()
						.map(this::getDependentProjects)
						.flatMap(Collection::stream).forEach(resolved::add);
				resolved = resolved.stream().distinct().sorted()
						.collect(Collectors.toList());
				resolved.removeIf(project -> !currentListener.project.depends
						.contains(project.name)
						&& project != currentListener.project);
				logger.debug("Building resolved projects {}", resolved);
				lastBuildException = null;
				for (CachingAntProject project : resolved) {
					if (Ax.isBlank(project.rebuildCommand)) {
						continue;
					}
					String script = null;
					if (project.rebuildCommand.startsWith("ant")) {
						script = Ax.format("cd %s && /usr/local/bin/%s",
								project.path, project.rebuildCommand);
					} else if (project.rebuildCommand.startsWith("bpx")) {
						script = Ax.format("/dk/%s", project.rebuildCommand);
					} else {
						throw new UnsupportedOperationException();
					}
					try {
						Output result = new Shell().runBashScript(script);
						result.throwOnException();
					} catch (Exception e) {
						lastBuildException = e;
						throw new WrappedRuntimeException(e);
					}
				}
				currentListener.pendingBuilds.clear();
			}
		}

		public void updateTasks(AntHandler antHandler) {
			this.antHandler = antHandler;
			if (currentTask != null
					&& currentTask.cwd.equals(runningTask.cwd)) {
				currentListener.flush();
				if (lastBuildException == null
						&& (runningTask.returnCmd.endsWith("hot-deploy")
								|| runningTask.returnCmd.endsWith("build-all"))
						&& !runningTask.returnCmd.contains("compile-gwt")) {
					runningTask.returnCmd = "";
				} else {
					try {
						// don't build until external process deletes lock
						new File(lockPath).createNewFile();
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			} else {
				if (currentListener != null) {
					currentListener.removeFsListeners();
				}
				currentTask = runningTask;
				CachingAntProject project = getProjectByCwd(runningTask.cwd);
				currentListener = new CachingAntListener(project);
				currentListener.addFsListeners();
			}
		}

		private List<CachingAntProject>
				getDependentProjects(CachingAntProject dependentsOf) {
			return projects.stream().filter(
					project -> project.depends.contains(dependentsOf.name))
					.collect(Collectors.toList());
		}

		private CachingAntProject getProjectByCwd(String cwd) {
			return projects.stream().filter(project -> project.path.equals(cwd))
					.findFirst().get();
		}

		private CachingAntProject getProjectByName(String name) {
			return projects.stream()
					.filter(project -> project.name.equals(name)).findFirst()
					.get();
		}

		private List<CachingAntProject>
				orderedDependentProjects(CachingAntProject project) {
			List<CachingAntProject> result = new ArrayList<>();
			result.add(project);
			project.depends.stream().map(this::getProjectByName)
					.forEach(result::add);
			result.sort(null);
			return result;
		}

		class CachingAntListener {
			private EventCollator seriesTimer = new EventCollator(
					200, new Runnable() {
						@Override
						public void run() {
							runBuildTargets();
						}
					}).withMaxDelayFromFirstEvent(99999);

			private CachingAntProject project;

			Set<CachingAntProject> pendingBuilds = new LinkedHashSet<>();

			List<AntListenerDirOsX> fsListeners = new ArrayList<>();

			public CachingAntListener(CachingAntProject project) {
				this.project = project;
			}

			public void addFsListeners() {
				logger.debug("Adding fs listeners - {}", project);
				List<CachingAntProject> dependentProjects = orderedDependentProjects(
						project);
				for (CachingAntProject dependentProject : dependentProjects) {
					Path listeningPath = Paths.get(dependentProject.path);
					try {
						AntListenerDirOsX listener = new AntListenerDirOsX(
								listeningPath, dependentProject);
						listener.trace = false;
						fsListeners.add(listener);
						new Thread() {
							@Override
							public void run() {
								try {
									listener.processEvents();
								} catch (Exception e) {
									throw new WrappedRuntimeException(e);
								}
							};
						}.start();
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
					logger.debug("Added fs listener - {} {}", dependentProject,
							dependentProject.path);
				}
			}

			public void addPendingBuild(CachingAntProject project) {
				pendingBuilds.add(project);
				seriesTimer.eventOccurred();
			}

			public void flush() {
				seriesTimer.cancel();
				runBuildTargets();
			}

			public void removeFsListeners() {
				fsListeners.forEach(WatchDirOsX::close);
			}

			class AntListenerDirOsX extends WatchDirOsX {
				private CachingAntProject project;

				AntListenerDirOsX(Path dir, CachingAntProject project)
						throws IOException {
					super(dir);
					this.project = project;
				}

				@Override
				protected void handleEvent(
						com.barbarysoftware.watchservice.WatchEvent<?> event,
						File file) {
					if (file.isDirectory()) {
						return;
					}
					String path = file.getPath();
					if (path.matches(".+\\.(jar|class|lock)")) {
						// build artifact
						return;
					}
					if (file.getPath().matches(
							".+/(?:build|bin|dist|classmeta/schema)/.+")) {
						// build path
						return;
					}
					if (file.getPath().matches(".+/api-request.*\\.json")) {
						// debug file
						return;
					}
					logger.debug("Intersting fs event - {} {}", project, path);
					addPendingBuild(project);
				}
			}
		}
	}

	static class CachingAntProject implements Comparable<CachingAntProject> {
		public String name;

		public String rebuildCommand;

		public String path;

		public List<String> depends = new ArrayList<>();

		@Override
		public int compareTo(CachingAntProject o) {
			if (this.dependsOn(o)) {
				return 1;
			}
			if (o.dependsOn(this)) {
				return -1;
			}
			return 0;
		}

		@Override
		public String toString() {
			return name;
		}

		private boolean dependsOn(CachingAntProject o) {
			return depends.contains(o.name);
		}
	}

	static class CachingAntTask {
		String returnCmd;

		String cmd;

		String cwd;

		public CachingAntTask(String cmd, String cwd) {
			this.cmd = cmd;
			this.cwd = cwd;
			this.returnCmd = cmd;
		}
	}
}

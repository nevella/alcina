package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskSetupAlcinaGwt extends PerformerTask {
	public String path;

	public boolean clean;

	public String gwtTag = "2.9.0";

	public String gitExec;

	public String alcinaRoot = "/g/alcina";

	public String java8sdkPath = "";

	public boolean compileGwt = false;

	File folder;

	File gwtRepoFolder;

	File gwtToolsFolder;

	List<SourcePath> alcinaSourcePaths;

	private List<SourcePath> alcinaPaths;

	private List<SourcePath> gwtPaths;

	@Override
	public void run() throws Exception {
		folder = new File(path);
		gwtRepoFolder = SEUtilities.getChildFile(folder, "gwt");
		gwtToolsFolder = SEUtilities.getChildFile(folder, "tools");
		if (clean) {
			SEUtilities.deleteDirectory(folder);
		}
		folder.mkdirs();
		gwtRepoFolder.mkdirs();
		gwtToolsFolder.mkdirs();
		cloneGwtRepo();
		cloneGwtToolsRepo();
		listAlcinaSourcePaths();
		listGwtSourcePaths();
		modifyTargetRepo();
		compileGwt();
	}

	void modifyTargetRepo() {
		Ax.out("gwtPaths: %s", gwtPaths.size());
		Ax.out("alcinaPaths: %s", alcinaPaths.size());
		MetricLogging.get().start("computePaths");
		alcinaPaths.forEach(p -> p.computePackageName());
		gwtPaths.forEach(p -> p.computePackageName());
		MetricLogging.get().end("computePaths");
	}

	void compileGwt() {
		if (!compileGwt) {
			return;
		}
		String cmd = Ax.format("JAVACMD=%s/bin/java && ant buildonly",
				java8sdkPath);
		Shell.execLogged(cmd);
	}

	void cloneGwtRepo() throws Exception {
		int existingFileCount = SEUtilities
				.listFilesRecursive(gwtRepoFolder.getAbsolutePath(), null, true)
				.size();
		if (existingFileCount != 0) {
			return;
		}
		{
			String cmd = Ax.format("cd %s && git clone %s",
					folder.getAbsolutePath(),
					"https://github.com/gwtproject/gwt.git");
			Shell.execLogged(cmd);
		}
		{
			String cmd = Ax.format("cd %s && git checkout %s",
					folder.getAbsolutePath(), gwtTag);
			Shell.execLogged(cmd);
		}
	}

	void cloneGwtToolsRepo() throws Exception {
		int existingFileCount = SEUtilities.listFilesRecursive(
				gwtToolsFolder.getAbsolutePath(), null, true).size();
		if (existingFileCount != 0) {
			return;
		}
		{
			String cmd = Ax.format("cd %s && git clone %s",
					folder.getAbsolutePath(),
					"https://github.com/gwtproject/tools.git");
			Shell.execLogged(cmd);
		}
	}

	void listAlcinaSourcePaths() throws Exception {
		alcinaPaths = SEUtilities.listFilesRecursive(alcinaRoot, null, true)
				.stream()
				.filter(f -> f.isFile() && f.getName().endsWith(".java"))
				.map(f -> new SourcePath(f, true)).collect(Collectors.toList());
	}

	void listGwtSourcePaths() throws Exception {
		gwtPaths = SEUtilities
				.listFilesRecursive(gwtRepoFolder.getAbsolutePath(), null, true)
				.stream().map(f -> new SourcePath(f, false))
				.collect(Collectors.toList());
	}

	static class SourcePath {
		String path;

		String packageName;

		String emulRoot;

		File file;

		boolean alcina;

		SourcePath(File file, boolean alcina) {
			this.file = file;
			this.alcina = alcina;
			path = file.getAbsolutePath();
		}

		void computePackageName() {
			String contents = Io.read().path(path).asString();
			packageName = contents.replaceFirst("(?s)^package (.+?);", "$1");
		}
	}
}

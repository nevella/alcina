package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Level;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.servlet.schedule.PerformerTask;

@TypeSerialization(reflectiveSerializable = false, flatSerializable = false)
public class TaskSetupAlcinaGwt extends PerformerTask {
	static class ClassKey {
		String className;

		boolean emul;

		String computedKey;

		ClassKey(String className, boolean emul) {
			this.className = className;
			this.emul = emul;
			computedKey = Ax.format("emul:%s::%s", emul ? "t" : "f", className);
		}

		@Override
		public int hashCode() {
			return computedKey.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassKey) {
				ClassKey o = (ClassKey) obj;
				return Objects.equals(computedKey, o.computedKey);
			}
			return super.equals(obj);
		}
	}

	enum GwtPathType {
		user_src, dev_core_src, user_super, dev_super, other
	}

	class ModifyTargetRepo {
		class PairedGwtAlcinaClass {
			SourcePath alcina;

			SourcePath gwt;

			ClassKey classKey;

			PairedGwtAlcinaClass(ClassKey classKey) {
				this.classKey = classKey;
			}

			GwtPathType pathType() {
				if (gwt == null || gwt.path
						.contains(gwtUserSrcFolder.getAbsolutePath())) {
					return GwtPathType.user_src;
				} else if (gwt.path
						.contains(gwtDevCoreSrcFolder.getAbsolutePath())) {
					return GwtPathType.dev_core_src;
				} else {
					return GwtPathType.other;
				}
			}

			void move() {
				File targetBase = null;
				switch (pathType()) {
				case other:
					// disabled so far
					return;
				case dev_core_src:
					targetBase = alcinaDevSrcTargetFolder;
					break;
				case user_src:
					targetBase = alcinaUserSrcTargetFolder;
					break;
				default:
					throw new UnsupportedOperationException();
				}
				String relativeSegment = alcina.path
						.replaceFirst(".+?/src(?:-regex|-element)?/(.+)", "$1");
				if (Objects.equals(relativeSegment, alcina.path)) {
					throw new IllegalStateException("Path: " + alcina.path);
				}
				File to = SEUtilities.getChildFile(targetBase, relativeSegment);
				to.getParentFile().mkdirs();
				Io.read().path(alcina.path).write().toFile(to);
				if (gwt != null) {
					new File(gwt.path).delete();
				}
			}

			void add(SourcePath path) {
				try {
					if (path.alcina) {
						Preconditions.checkState(alcina == null);
						alcina = path;
					} else {
						Preconditions.checkState(gwt == null);
						gwt = path;
					}
				} catch (Exception e) {
					logger.warn("Duplicate path - {}", classKey.className);
					throw WrappedRuntimeException.wrap(e);
				}
			}
		}

		File alcinaDevSrcTargetFolder;

		File alcinaUserSrcTargetFolder;

		File gwtDevCoreSrcFolder;

		File gwtUserSrcFolder;

		List<PairedGwtAlcinaClass> toSync;

		CachingMap<ClassKey, PairedGwtAlcinaClass> pairs = new CachingMap<>(
				PairedGwtAlcinaClass::new);

		void run() {
			Ax.out("gwtPaths: %s", gwtPaths.size());
			Ax.out("alcinaPaths: %s", alcinaPaths.size());
			MetricLogging.get().start("computePaths");
			alcinaPaths.forEach(p -> p.computePackageName());
			gwtPaths.forEach(p -> p.computePackageName());
			MetricLogging.get().end("computePaths");
			/*
			 * Ensure alcina dirs in dev, user
			 * 
			 * Pair gwtrepo, alcinarepo classes. Note - gwtrepo can either be in
			 * alcinasrc or gwtsrc folder Í Remove gwtrepo, copy alcinarepo
			 * 
			 * Update build files where approp
			 * 
			 */
			setupDirs();
			pairSourcePaths();
			modifySources();
			logger.info("Paired source paths: {}", toSync.size());
		}

		void modifySources() {
			toSync.forEach(PairedGwtAlcinaClass::move);
		}

		void pairSourcePaths() {
			alcinaPaths.forEach(p -> pairs.get(p.getClassKey()).add(p));
			gwtPaths.forEach(p -> pairs.get(p.getClassKey()).add(p));
			// FIXME - add emuls
			toSync = pairs.values().stream().filter(p -> p.alcina != null
					&& (p.gwt != null || (p.alcina.packageName
							.startsWith("com.google.gwt") && !p.alcina.emul)))
					.collect(Collectors.toList());
		}

		void setupDirs() {
			alcinaDevSrcTargetFolder = SEUtilities.getChildFile(gwtRepoFolder,
					"dev/alcina/src");
			alcinaUserSrcTargetFolder = SEUtilities.getChildFile(gwtRepoFolder,
					"user/alcina/src");
			gwtDevCoreSrcFolder = SEUtilities.getChildFile(gwtRepoFolder,
					"dev/core/src");
			gwtUserSrcFolder = SEUtilities.getChildFile(gwtRepoFolder,
					"user/src");
			// could also modify the build.xml targets - but doing that by hand
			// for the mo'
		}
	}

	static class SourcePath {
		String path;

		String packageName;

		String emulRoot;

		File file;

		boolean emul;

		boolean alcina;

		SourcePath(File file, boolean alcina) {
			this.file = file;
			this.alcina = alcina;
			path = file.getAbsolutePath();
			emul = path.matches(".*/(emul|translatable|super)/.*");
		}

		public ClassKey getClassKey() {
			return new ClassKey(Ax.format("%s.%s", packageName,
					file.getName().replace(".java", "")), emul);
		}

		void computePackageName() {
			String contents = Io.read().path(path).asString();
			Pattern p = Pattern.compile("(?s)(?:\\A|\n)package (.+?);");
			Matcher m = p.matcher(contents);
			if (m.find()) {
				packageName = m.group(1);
			} else {
				throw new IllegalArgumentException(
						Ax.format("Can't determine package: %s", path));
			}
		}
	}

	public String path;

	public boolean clean;

	public String gwtTag = "release/2.9.0";

	public String branchTo = "alc-gwt/0.1";

	public String gitExec;

	public String alcinaRoot = "/g/alcina";

	public String java8sdkPath = "";

	public String excludeAlcinaSegments = ".*/extras/rpc/.*";

	public String excludeGwtSegments = ".*/elemental/src/.*";

	public boolean compileGwt = false;

	File folder;

	File gwtRepoFolder;

	File gwtToolsFolder;

	List<SourcePath> alcinaSourcePaths;

	private List<SourcePath> alcinaPaths;

	private List<SourcePath> gwtPaths;

	@Override
	public void run() throws Exception {
		EntityLayerLogging.setLevel(MetricLogging.class, Level.DEBUG);
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
		new ModifyTargetRepo().run();
		compileGwt();
	}

	void compileGwt() {
		if (!compileGwt) {
			return;
		}
		String cmd = Ax.format("cd %s && JAVACMD=%s/bin/java && ant buildonly",
				gwtRepoFolder, java8sdkPath);
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
					"https://github.com/nevella/gwt.git");
			Shell.execLogged(cmd);
		}
		{
			String cmd = Ax.format("cd %s && git checkout %s",
					gwtRepoFolder.getAbsolutePath(), gwtTag);
			Shell.execLogged(cmd);
		}
		{
			String cmd = Ax.format("cd %s && git checkout -b %s",
					gwtRepoFolder.getAbsolutePath(), branchTo);
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
				.filter(f -> !f.getAbsolutePath()
						.matches(excludeAlcinaSegments))
				.map(f -> new SourcePath(f, true)).collect(Collectors.toList());
	}

	void listGwtSourcePaths() throws Exception {
		gwtPaths = SEUtilities
				.listFilesRecursive(gwtRepoFolder.getAbsolutePath(), null, true)
				.stream()
				.filter(f -> f.isFile() && f.getName().endsWith(".java")
						&& !f.getAbsolutePath().contains("testdata")
						&& !f.getAbsolutePath().contains("user/test"))
				.filter(f -> !f.getAbsolutePath().matches(excludeGwtSegments))
				.map(f -> new SourcePath(f, false))
				.collect(Collectors.toList());
	}
}

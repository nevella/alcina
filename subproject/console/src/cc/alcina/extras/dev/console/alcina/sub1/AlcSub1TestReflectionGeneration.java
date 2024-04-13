package cc.alcina.extras.dev.console.alcina.sub1;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.extras.dev.console.alcina.AlcinaDevConsole;
import cc.alcina.extras.dev.console.alcina.AlcinaDevConsoleRunnable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.gwt.reflection.jdk.JdkReflectionGenerator;
import cc.alcina.framework.entity.util.Shell;

public class AlcSub1TestReflectionGeneration extends AlcinaDevConsoleRunnable {
	@Override
	public void run() throws Exception {
		JdkReflectionGenerator.Attributes attributes = JdkReflectionGenerator
				.attributes();
		attributes.clean = true;
		attributes.outputRoot = "/tmp/log/alc/reflection";
		attributes.loadClassDirectoryPaths(Entity.class,
				AlcinaDevConsole.class);
		String indexFilePath = Ax.format(
				"%s/src/cc/alcina/framework/common/client/reflection/ModuleReflector_Initial_Impl.java",
				attributes.outputRoot);
		attributes.build().generate();
		Ax.out("Reflected classes generated:\n\tAll: %s\n\tIndex: %s",
				attributes.outputRoot, indexFilePath);
		/*
		 * Generate class files
		 */
		String classPathStr = System.getProperty("java.class.path");
		String outputSourceRoot = Ax.format("%s/src", attributes.outputRoot);
		String outputBinRoot = Ax.format("%s/bin", attributes.outputRoot);
		String sourceListPath = Ax.format("%s/src.txt", attributes.outputRoot);
		List<File> sourcePaths = SEUtilities
				.listFilesRecursive(outputSourceRoot, null, true).stream()
				.filter(f -> f.getName().endsWith(".java"))
				.collect(Collectors.toList());
		String sourceList = sourcePaths.stream().map(File::getPath)
				.collect(Collectors.joining("\n"));
		Io.write().string(sourceList).toPath(sourceListPath);
		String cmd = Ax.format("javac -cp %s -d %s @%s", classPathStr,
				outputBinRoot, sourceListPath);
		try {
			Shell.execLogged(cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

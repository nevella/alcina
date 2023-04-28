package cc.alcina.extras.dev.console.code;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.ZipUtil;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskGenerateGwtTypemodelJar extends PerformerTask {
	transient String outPath = "/tmp/gwt-typemodel";

	@Override
	public void run() throws Exception {
		String path = "/g/alcina/lib/framework/gwt/gwt-dev.jar";
		File outFolder = new File(outPath);
		File outJar = new File(outPath + ".jar");
		SEUtilities.deleteDirectory(outFolder);
		outFolder.mkdirs();
		new ZipUtil().unzip(outFolder,
				new BufferedInputStream(new FileInputStream(path)));
		List<File> files = SEUtilities.listFilesRecursive(outPath, null);
		files.stream().filter(f -> f.isFile()).filter(f -> !this.isTypeModel(f))
				.forEach(File::delete);
		List<File> list = files.stream().filter(f -> f.isDirectory())
				.filter(f -> !this.isTypeModel(f))
				.sorted(Comparator.comparing(f -> f.getPath().length()))
				.collect(Collectors.toList());
		Collections.reverse(list);
		list.forEach(File::delete);
		new ZipUtil().createZip(outJar, outFolder, Collections.emptyMap());
	}

	boolean isTypeModel(File f) {
		String packageFragment = "com.google.gwt.core.ext.typeinfo".replace(".",
				"/");
		String pathFilter = f.getAbsolutePath().replace(outPath + "/", "");
		return packageFragment.contains(pathFilter)
				|| pathFilter.contains(packageFragment);
	}
}

package cc.alcina.extras.dev.console.code;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Configuration.ConfigurationFile;
import cc.alcina.framework.entity.Configuration.PropertyTree;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.servlet.schedule.ServerTask;

@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class TaskRefactorConfigSets extends ServerTask {
	private List<String> classpathEntries = new ArrayList<>();

	private List<Configuration.ConfigurationFile> appPropertyFileEntries = new ArrayList<>();

	private String classpathConfigurationFileFilter = "Bundle\\.properties";

	transient List<Configuration.ConfigurationFile> classpathConfigurationFiles = new ArrayList<>();

	private PropertyTree tree;

	private Function<String, String> classpathEntryToSet;

	public List<Configuration.ConfigurationFile> getAppPropertyFileEntries() {
		return this.appPropertyFileEntries;
	}

	public String getClasspathConfigurationFileFilter() {
		return this.classpathConfigurationFileFilter;
	}

	public List<String> getClasspathEntries() {
		return this.classpathEntries;
	}

	public Function<String, String> getClasspathEntryToSet() {
		return this.classpathEntryToSet;
	}

	@Override
	public void run() throws Exception {
		locateConfigurationFiles();
		checkConfigurationFilesValid();
		populateTree();
		String csv = tree.asCsv();
		Io.write().string(csv).toPath("/tmp/tree.csv");
	}

	public void setAppPropertyFileEntries(
			List<Configuration.ConfigurationFile> appPropertyFileEntries) {
		this.appPropertyFileEntries = appPropertyFileEntries;
	}

	public void setClasspathConfigurationFileFilter(
			String classpathConfigurationFileFilter) {
		this.classpathConfigurationFileFilter = classpathConfigurationFileFilter;
	}

	public void setClasspathEntries(List<String> classpathEntries) {
		this.classpathEntries = classpathEntries;
	}

	public void setClasspathEntryToSet(
			Function<String, String> classpathEntryToSet) {
		this.classpathEntryToSet = classpathEntryToSet;
	}

	private void checkConfigurationFilesValid() {
		List<ConfigurationFile> nonNamespaced = classpathConfigurationFiles
				.stream()
				.filter(ConfigurationFile::provideContainsNonNamespaced)
				.collect(Collectors.toList());
		if (nonNamespaced.size() > 0) {
			Ax.out(nonNamespaced);
			throw new UnsupportedOperationException();
		}
	}

	/*
	 * Traverse classpathentries
	 *
	 * Scane for matching filter
	 *
	 * Construct - relative path == package
	 *
	 *
	 */
	private void locateConfigurationFiles() {
		for (String path : classpathEntries) {
			Stack<String> folders = new Stack<>();
			folders.push(path);
			while (folders.size() > 0) {
				String folder = folders.pop();
				File[] files = new File(folder).listFiles();
				for (File file : files) {
					if (file.isDirectory()) {
						folders.push(file.getPath());
					} else {
						if (file.getName()
								.matches(classpathConfigurationFileFilter)) {
							ConfigurationFile configurationFile = new Configuration.ConfigurationFile(
									path, file,
									classpathEntryToSet.apply(path));
							classpathConfigurationFiles.add(configurationFile);
						}
					}
				}
			}
		}
	}

	private void populateTree() {
		this.tree = new Configuration.PropertyTree();
		classpathConfigurationFiles.forEach(tree::add);
		appPropertyFileEntries.forEach(tree::add);
	}
}

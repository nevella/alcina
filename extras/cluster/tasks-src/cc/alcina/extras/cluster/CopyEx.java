package cc.alcina.extras.cluster;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileNameMapper;

/*
 * a complement to javacex - so that mvcc/classtransformer has the correct sources 
 */
public class CopyEx extends Copy {
	List<FileSet> orderedFileSets = new ArrayList<>();

	List<BuildMapParams> buildMapParamsList = new ArrayList<>();

	@Override
	public void addFileset(FileSet set) {
		super.addFileset(set);
		orderedFileSets.add(set);
	}

	@Override
	public void execute() throws BuildException {
		setOverwrite(true);
		super.execute();
	}

	@Override
	protected void buildMap(File fromDir, File toDir, String[] names,
			FileNameMapper mapper, Hashtable map) {
		{
			BuildMapParams params = new BuildMapParams(fromDir, toDir, names,
					mapper, map);
			buildMapParamsList.add(params);
		}
		if (buildMapParamsList.size() == orderedFileSets.size()) {
			Map<String, String> toSrc = new LinkedHashMap<>();
			for (int idx = orderedFileSets.size() - 1; idx >= 0; idx--) {
				FileSet filest = orderedFileSets.get(idx);
				// run in order, last overrides
				for (BuildMapParams params : buildMapParamsList) {
					if (params.fromDir.equals(filest.getDir())) {
						Hashtable<String, String[]> typedMap = map;
						// checks for difference (not just newer-than). if there
						// are multiple
						// matches, uses only the first (note, iterating in
						// reversed order over ordered file sets).
						fromDir = params.fromDir;
						toDir = params.toDir;
						names = params.names;
						mapper = params.mapper;
						map = params.map;
						for (int i = 0; i < names.length; i++) {
							String name = names[i];
							if (mapper.mapFileName(name) != null) {
								final File src = new File(fromDir, name);
								final String[] mappedFiles = mapper
										.mapFileName(name);
								File toFile = new File(toDir, mappedFiles[0]);
								String toPath = toFile.getAbsolutePath();
								String srcPath = src.getAbsolutePath();
								boolean unchanged = toFile.exists()
										&& Math.abs(src.lastModified()
												- toFile.lastModified()) < 100
										&& src.length() == toFile.length();
								if (toSrc.containsKey(toPath)) {
									log("[-] " + srcPath, Project.MSG_DEBUG);
								} else {
									toSrc.put(toPath, srcPath);
									if (!unchanged) {
										log("[+] " + srcPath,
												Project.MSG_DEBUG);
										map.put(srcPath, new String[] {
												toFile.getAbsolutePath() });
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private class BuildMapParams {
		private File fromDir;

		private File toDir;

		private String[] names;

		private FileNameMapper mapper;

		private Hashtable map;

		public BuildMapParams(File fromDir, File toDir, String[] names,
				FileNameMapper mapper, Hashtable map) {
			this.fromDir = fromDir;
			this.toDir = toDir;
			this.names = names;
			this.mapper = mapper;
			this.map = map;
		}
	}
}

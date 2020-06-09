package cc.alcina.extras.cluster;

import java.io.File;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.util.FileNameMapper;

public class CopyEx extends Copy {
	Set<String> preserve = new LinkedHashSet<>();

	@Override
	public void execute() throws BuildException {
		setOverwrite(true);
		super.execute();
		preserve.clear();
	}

	@Override
	protected void buildMap(File fromDir, File toDir, String[] names,
			FileNameMapper mapper, Hashtable map) {
		Hashtable<String, String[]> typedMap = map;
		// checks for difference (not just newer-than). if there are multiple
		// matches, uses only the last
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			if (mapper.mapFileName(name) != null) {
				final File src = new File(fromDir, name);
				final String[] mappedFiles = mapper.mapFileName(name);
				File toFile = new File(toDir, mappedFiles[0]);
				if (Math.abs(src.lastModified() - toFile.lastModified()) < 100
						&& src.length() == toFile.length()) {
					preserve.add(toFile.getAbsolutePath());
				} else {
					if (preserve.contains(toFile.getAbsolutePath())) {
						log("[-] " + src.getAbsolutePath());
					} else {
						log("[+] " + src.getAbsolutePath());
						map.put(src.getAbsolutePath(),
								new String[] { toFile.getAbsolutePath() });
					}
				}
			}
		}
	}
}

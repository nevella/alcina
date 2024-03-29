package cc.alcina.extras.cluster;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;

public class BackupFiles extends Task {
	public static void writeStreamToStream(InputStream is, OutputStream os)
			throws IOException {
		BufferedOutputStream fos = new BufferedOutputStream(os);
		InputStream in = new BufferedInputStream(is);
		int bufLength = 8192;
		byte[] buffer = new byte[bufLength];
		int result;
		while ((result = in.read(buffer)) != -1) {
			fos.write(buffer, 0, result);
		}
		fos.flush();
		fos.close();
	}

	private Vector filesets = new Vector();

	private String backupPath;

	private int maxBackups;

	private boolean flatten;

	private int maxDays;

	public void addFileset(FileSet fileset) {
		filesets.add(fileset);
	}

	@Override
	public void execute() throws BuildException {
		try {
			execute0();
		} catch (NoClassDefFoundError err) {
			// ignore, is ok
			System.out.println(err.getMessage());
		}
	}

	public void execute0() throws BuildException {
		File folder = new File(backupPath);
		folder.mkdirs();
		FileFilter greaterThanMaxDaysFilter = new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory()
						|| file.lastModified() < System.currentTimeMillis()
								- ((long) maxDays) * 1000 * 86400;
			}
		};
		if (!isFlatten()) {
			rotate(folder);
		} else {
			if (maxDays > 0) {
				for (File file : Utils.listFilesRecursive(backupPath,
						greaterThanMaxDaysFilter, true)) {
					file.delete();
				}
			}
		}
		File target = Utils.getChildFile(folder, "contents.1");
		target.mkdirs();
		int fc = 0;
		try {
			final FileSet files = (FileSet) filesets.get(0);
			File root = files.getDir(getProject());
			Iterator itr = files.iterator();
			for (; itr.hasNext();) {
				FileResource resource = (FileResource) itr.next();
				if (!greaterThanMaxDaysFilter.accept(resource.getFile())) {
					String rel = resource.getFile().getPath()
							.substring(root.getPath().length() + 1);
					File targetChild = Utils.getChildFile(target, rel);
					targetChild.getParentFile().mkdirs();
					fc += Utils.copyFile(resource.getFile(), targetChild);
				}
			}
			log(String.format("Backed up %s files", fc));
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	public String getBackupPath() {
		return this.backupPath;
	}

	public int getMaxBackups() {
		return this.maxBackups;
	}

	public int getMaxDays() {
		return maxDays;
	}

	public boolean isFlatten() {
		return this.flatten;
	}

	protected void rotate(File folder) {
		final Pattern np = Pattern.compile("(.+)(\\.\\d+)");
		ArrayList<File> backups = new ArrayList<File>(
				Arrays.asList(folder.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return np.matcher(name).matches();
					}
				})));
		Collections.sort(backups, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.lastModified() < o2.lastModified() ? -1
						: o1.lastModified() == o2.lastModified()
								? -o1.getName().compareTo(o2.getName())
								: 1;
			}
		});
		for (int i = backups.size() - 1; i >= 0; i--) {
			File f = backups.get(i);
			if (i >= maxBackups - 1) {
				f.delete();
			} else {
				String name = f.getName();
				Matcher m = np.matcher(name);
				m.matches();
				f.renameTo(new File(f.getParent() + File.separator + m.group(1)
						+ "." + (i + 2)));
			}
		}
	}

	public void setBackupPath(String backupPath) {
		this.backupPath = backupPath;
	}

	public void setFlatten(boolean flatten) {
		this.flatten = flatten;
	}

	public void setMaxBackups(int maxBackups) {
		this.maxBackups = maxBackups;
	}

	public void setMaxDays(int maxDays) {
		this.maxDays = maxDays;
	}
}

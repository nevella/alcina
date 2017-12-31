package cc.alcina.extras.cluster;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class BackupFile extends Task {
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

	private String filePath;

	private String backupPath;

	private int maxBackups;

	@Override
	public void execute() throws BuildException {
		File file = new File(backupPath);
		file.mkdirs();
		File in = new File(filePath);
		File out = new File(backupPath + File.separator + in.getName() + ".1");
		if (in.exists() && out.exists() && in.length() == out.length()
				&& in.lastModified() == out.lastModified()) {
			log("no change in backup files");
		}
		final Pattern np = Pattern.compile("(.+)(\\.\\d+)");
		ArrayList<File> backups = new ArrayList<File>(
				Arrays.asList(file.listFiles(new FilenameFilter() {
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
								? -o1.getName().compareTo(o2.getName()) : 1;
			}
		});
		Collections.reverse(backups);
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
		try {
			if (in.exists()) {
				FileInputStream ins = new FileInputStream(in);
				FileOutputStream os = new FileOutputStream(out);
				writeStreamToStream(ins, os);
				out.setLastModified(in.lastModified());
				ins.close();
			}
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	public String getBackupPath() {
		return this.backupPath;
	}

	public String getFilePath() {
		return this.filePath;
	}

	public int getMaxBackups() {
		return this.maxBackups;
	}

	public void setBackupPath(String backupPath) {
		this.backupPath = backupPath;
	}

	public void setFilePath(String earPath) {
		this.filePath = earPath;
	}

	public void setMaxBackups(int maxBackups) {
		this.maxBackups = maxBackups;
	}
}

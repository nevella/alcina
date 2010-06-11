package cc.alcina.appcreator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Stack;

import org.apache.tools.ant.Project;

class PackageUtils {
	public static String readFileToString(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		return readStreamToString(fis);
	}

	public static String readStreamToString(InputStream is) throws IOException {
		return readStreamToString(is, null);
	}

	public static String readStreamToString(InputStream is, String charsetName)
			throws IOException {
		charsetName = charsetName == null ? "UTF-8" : charsetName;
		BufferedReader in = new BufferedReader(new InputStreamReader(is,
				charsetName));
		StringWriter sw = new StringWriter();
		char[] cb = new char[4096];
		int len = -1;
		while ((len = in.read(cb, 0, 4096)) != -1) {
			sw.write(cb, 0, len);
		}
		return sw.toString();
	}

	public static String folderOf(String path) {
		int x = path.lastIndexOf('/');
		return x == -1 ? path : path.substring(0, x);
	}

	public static String fileOf(String path) {
		int x = path.lastIndexOf('/');
		return x == -1 ? path : path.substring(x + 1);
	}

	public static boolean isNullOrEmpty(String string) {
		return string == null || string.length() == 0;
	}

	public static boolean clearFolder(File folder) {
		Stack<File> pathStack = new Stack<File>();
		Stack<File> dirStack = new Stack<File>();
		pathStack.push(folder);
		dirStack.push(folder);
		while (pathStack.size() != 0) {
			File dInf = pathStack.pop();
			File[] files = dInf.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					pathStack.push(file);
					dirStack.push(file);
				} else {
					boolean b = file.delete();
					if (!b) {
						return b;
					}
				}
			}
		}
		while (dirStack.size() != 1) {
			File dInf = dirStack.pop();
			boolean b = dInf.delete();
			if (!b) {
				return b;
			}
		}
		return true;
	}

	public static File findFile(File folder, final String fileName) {
		File[] files = folder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return dir.isDirectory() || name.equals(fileName);
			}
		});
		for (File file : files) {
			if (file.isDirectory()) {
				return findFile(file, fileName);
			} else {
				return file;
			}
		}
		return null;
	}

	public static boolean prune(File folder) {
		File[] files = folder.listFiles();
		boolean delete = true;
		for (File file : files) {
			if (file.isDirectory()) {
				delete &= prune(file);
			} else {
				delete = false;
			}
		}
		if (delete) {
			folder.delete();
		}
		return delete;
	}

	public static String camelCase(String string) {
		return string.substring(0, 1).toLowerCase() + string.substring(1);
	}

	@SuppressWarnings("deprecation")
	public static String translatePath( String path) {
		return Project.translatePath(path);
	}
}

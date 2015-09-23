package cc.alcina.extras.cluster;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

class Utils {
	public static List<File> listFilesRecursive(String initialPath,
			FileFilter filter, boolean removeFolders) {
		return listFilesRecursive(initialPath, filter, removeFolders, null);
	}

	public static List<File> listFilesRecursive(String initialPath,
			FileFilter filter, boolean removeFolders,
			Pattern doNotCheckFolderPattern) {
		Stack<File> folders = new Stack<File>();
		List<File> results = new ArrayList<File>();
		folders.add(new File(initialPath));
		while (!folders.isEmpty()) {
			File folder = folders.pop();
			File[] files = filter == null ? folder.listFiles() : folder
					.listFiles(filter);
			for (File file : files) {
				if (doNotCheckFolderPattern == null
						|| !doNotCheckFolderPattern.matcher(file.getName())
								.matches()) {
					if (file.isDirectory()) {
						folders.push(file);
					}
				}
				results.add(file);
			}
		}
		if (removeFolders) {
			for (Iterator<File> itr = results.iterator(); itr.hasNext();) {
				File file = itr.next();
				if (doNotCheckFolderPattern == null
						|| !doNotCheckFolderPattern.matcher(file.getName())
								.matches()) {
					if (file.isDirectory()) {
						itr.remove();
					}
				}
			}
		}
		return results;
	}

	public static File getChildFile(File folder, String childFileName) {
		return new File(String.format("%s/%s", folder.getPath(), childFileName));
	}

	public static int copyFile(File in, File out) throws IOException {
		if (in.isDirectory()) {
			return copyDirectory(in, out);
		}
		if (!out.exists()) {
			out.getParentFile().mkdirs();
			out.createNewFile();
		} else {
			if (out.lastModified() >= in.lastModified()) {
				return 0;
			}
		}
		FileInputStream ins = new FileInputStream(in);
		FileOutputStream os = new FileOutputStream(out);
		writeStreamToStream(ins, os);
		out.setLastModified(in.lastModified());
		ins.close();
		return 1;
	}

	public static void writeStreamToStream(InputStream is, OutputStream os)
			throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(os);
		InputStream in = new BufferedInputStream(is);
		int bufLength = 8192;
		byte[] buffer = new byte[bufLength];
		int result;
		while ((result = in.read(buffer)) != -1) {
			bos.write(buffer, 0, result);
		}
		bos.flush();
		bos.close();
		is.close();
	}

	private static int copyDirectory(File in, File out) throws IOException {
		int fc = 0;
		if (out.exists()) {
			if (out.isDirectory()) {
				deleteDirectory(out);
			} else {
				out.delete();
			}
		}
		out.mkdirs();
		File[] files = in.listFiles();
		for (File subIn : files) {
			File subOut = new File(out.getPath() + File.separator
					+ subIn.getName());
			fc += copyFile(subIn, subOut);
		}
		return fc;
	}

	public static boolean deleteDirectory(File folder) {
		if (!folder.exists()) {
			return false;
		}
		if (!folder.isDirectory()) {
			return folder.delete();
		}
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
		while (dirStack.size() != 0) {
			File dInf = dirStack.pop();
			boolean b = dInf.delete();
			if (!b) {
				return b;
			}
		}
		return true;
	}

	public static String readUrlAsString(String strUrl) throws Exception {
		URL url = new URL(strUrl);
		InputStream is = null;
		is = url.openConnection().getInputStream();
		String input = readStreamToString(is, "UTF-8");
		return input;
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
		is.close();
		return sw.toString();
	}
}

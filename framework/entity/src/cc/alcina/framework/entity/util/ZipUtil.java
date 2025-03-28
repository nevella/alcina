/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.entity.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;

/**
 *
 * @author Nick Reddel
 */
public class ZipUtil {
	public static byte[] gunzipBytes(byte[] bytes) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPInputStream gzipInputStream = new GZIPInputStream(
					new ByteArrayInputStream(bytes));
			Io.Streams.copy(gzipInputStream, baos);
			return baos.toByteArray();
		} catch (Exception e) {
			return bytes;
		}
	}

	public static byte[] gzipBytes(byte[] bytes) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);
			gzipOutputStream.write(bytes);
			gzipOutputStream.flush();
			gzipOutputStream.close();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static void writeStreamToStream(InputStream in, OutputStream os,
			boolean keepOutputOpen) throws IOException {
		OutputStream bos = os instanceof ByteArrayOutputStream ? os
				: new BufferedOutputStream(os);
		int bufLength = in.available() <= 1024 ? 1024 * 64
				: Math.min(1024 * 1024, in.available());
		byte[] buffer = new byte[bufLength];
		int result;
		while ((result = in.read(buffer)) != -1) {
			bos.write(buffer, 0, result);
		}
		bos.flush();
		if (!keepOutputOpen) {
			bos.close();
		}
		in.close();
	}

	public void createZip(File outputFile, File root,
			Map<String, File> substitutes) throws Exception {
		String dInfMname = root.getAbsolutePath();
		ZipOutputStream s = new ZipOutputStream(
				new FileOutputStream(outputFile));
		s.setLevel(5);
		List<File> files = new ArrayList<File>();
		Stack<File> pathStack = new Stack<File>();
		pathStack.push(root);
		while (pathStack.size() != 0) {
			File f = pathStack.pop();
			if (f.isFile()) {
				files.add(f);
			} else {
				File[] filez = f.listFiles();
				for (File file : filez) {
					if (file.isDirectory()) {
						pathStack.push(file);
					}
					if (!file.equals(root)) {
						files.add(file);
					}
				}
			}
		}
		for (File f : files) {
			String subName = null;
			if (f == root) {
				subName = f.getName();
			} else {
				subName = f.getPath().substring(dInfMname.length() + 1);
			}
			if (f.isDirectory()) {
				subName += "/";
			}
			if (substitutes.containsKey(subName)) {
				f = substitutes.get(subName);
			}
			ZipEntry entry = new ZipEntry(subName);
			entry.setTime(f.lastModified());
			s.putNextEntry(entry);
			if (!f.isDirectory()) {
				FileInputStream fs = new FileInputStream(f);
				writeStreamToStream(fs, s, true);
			}
		}
		s.finish();
		s.close();
	}

	public void unzip(File outputFolder, InputStream zipStream)
			throws Exception {
		if (zipStream instanceof ZipInputStream) {
			throw new IllegalArgumentException();
		}
		ZipInputStream s = new ZipInputStream(zipStream);
		ZipEntry theEntry;
		while ((theEntry = s.getNextEntry()) != null) {
			String outputFn = outputFolder.getPath() + File.separator
					+ theEntry.getName();
			String seprEsc = (File.separator.equals("\\")) ? "\\" + "\\" : "/";
			outputFn = outputFn.replace('\\', '/').replaceAll("/", seprEsc);
			int x = outputFn.lastIndexOf(File.separator);
			String directoryName = outputFn.substring(0, x);
			String fileName = outputFn.substring(x + 1);
			// create directory
			File oDir = new File(directoryName);
			if (!oDir.exists()) {
				oDir.mkdirs();
			}
			if (theEntry.isDirectory()) {
				oDir.setLastModified(theEntry.getTime());
			} else {
				FileOutputStream streamWriter = null;
				int suffixCount = 0;
				File file = null;
				while (true) {
					String suffix = suffixCount++ == 0 ? "" : "-" + suffixCount;
					String path = outputFn + suffix;
					file = new File(path);
					if (!file.exists()) {
						file.createNewFile();
						streamWriter = new FileOutputStream(path);
						break;
					}
				}
				int size = 2048;
				byte[] data = new byte[2048];
				while (true) {
					size = s.read(data, 0, data.length);
					if (size > 0) {
						streamWriter.write(data, 0, size);
					} else {
						break;
					}
				}
				streamWriter.close();
				file.setLastModified(theEntry.getTime());
			}
		}
		s.close();
	}

	public static void createZip(File outputFile, List<File> inputFiles) {
		try {
			Path tempDirWithPrefix = Files.createTempDirectory("zip");
			File tempDir = tempDirWithPrefix.toFile();
			for (File input : inputFiles) {
				SEUtilities.copyFile(input, tempDir);
			}
			new ZipUtil().createZip(outputFile, tempDir, Map.of());
			SEUtilities.deleteDirectory(tempDir);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}

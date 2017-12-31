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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Nick Reddel
 */
public class ZipUtil {
	public void createZip(File outputFile, File containerFolder,
			Map<String, File> substitutes) throws Exception {
		String dInfMname = containerFolder.getAbsolutePath();
		ZipOutputStream s = new ZipOutputStream(
				new FileOutputStream(outputFile));
		s.setLevel(5);
		List<File> files = new ArrayList<File>();
		Stack<File> pathStack = new Stack<File>();
		pathStack.push(containerFolder);
		while (pathStack.size() != 0) {
			File f = pathStack.pop();
			File[] filez = f.listFiles();
			for (File file : filez) {
				if (file.isDirectory()) {
					pathStack.push(file);
				}
				if (!file.equals(containerFolder)) {
					files.add(file);
				}
			}
		}
		for (File f : files) {
			String subName = f.getPath().substring(dInfMname.length() + 1);
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
				byte[] buffer = new byte[(int) f.length()];
				fs.read(buffer, 0, buffer.length);
				s.write(buffer, 0, buffer.length);
				fs.close();
			}
		}
		s.finish();
		s.close();
	}

	public void unzip(File outputFolder, InputStream zipStream)
			throws Exception {
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
			File inf = new File(outputFn);
			if (fileName.length() != 0) {
				if (inf.exists()) {
					// unset readonly inf.
				} else {
					inf.createNewFile();
				}
				FileOutputStream streamWriter = new FileOutputStream(outputFn);
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
				inf.setLastModified(theEntry.getTime());
			}
		}
		s.close();
	}
}

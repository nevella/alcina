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
package cc.alcina.framework.servlet.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;

/**
 *
 * @author Nick Reddel
 */
public class DownloadServlet extends HttpServlet {
	private static Map<String, DownloadItem> items = Collections
			.synchronizedMap(new LinkedHashMap<String, DownloadItem>());

	public static String add(DownloadItem item) {
		String id = SEUtilities.generateId();
		Ax.out("Added download at /downloadServlet.do?id=%s", id);
		items.put(id, item);
		return id;
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse res) {
		doPost(request, res);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse res) {
		final String id = request.getParameter("id");
		if (!items.containsKey(id)) {
			System.out.println("Download request not served: " + id);
			res.setStatus(404);
			return;
		}
		System.out.println("Download request served: " + id);
		DownloadItem item = items.get(id);
		final File f = new File(item.tmpFileName);
		if (request.getParameter("noheaders") == null) {
			res.setContentType(item.mimeType);
			res.setContentLength((int) f.length());
			if (item.fileName != null) {
				String fixedFileName = item.fileName
						.replaceAll("[^\\x20-\\x7e]", "");
				res.setHeader("Content-Disposition",
						"attachment; filename=\"" + fixedFileName + "\"");
			}
		}
		// FIXME - dirndl1.2 - multiple timers here
		try {
			ResourceUtilities.writeStreamToStream(
					new BufferedInputStream(new FileInputStream(f)),
					res.getOutputStream());
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						items.remove(id);
						f.delete();
					} catch (Exception e) {
						// webapp restart
					}
				}
			}, 60 * 1000);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public File getFile(String id) {
		DownloadItem item = items.get(id);
		if (item != null) {
			return new File(item.tmpFileName);
		}
		return null;
	}

	public DownloadItem getItem(String id) {
		return items.get(id);
	}

	public static class DownloadItem {
		private final String mimeType;

		private final String fileName;

		private final String tmpFileName;

		public DownloadItem(String mimeType, String fileName,
				String tmpFileName) {
			this.mimeType = mimeType;
			this.fileName = fileName;
			this.tmpFileName = tmpFileName;
		}
	}
}

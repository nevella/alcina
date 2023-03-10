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
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer.ReflectiveSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.SimpleHttp;

/**
 *
 * @author Nick Reddel
 */
public class DownloadServlet extends HttpServlet {
	private static final String PAYLOAD = "payload";

	private static final String REGISTER_DOWNLOAD = "registerDownload";

	private static Map<String, DownloadItem> items = Collections
			.synchronizedMap(new LinkedHashMap<String, DownloadItem>());

	public static String add(DownloadItem item) {
		items.put(item.id, item);
		Ax.out("Added download at /downloadServlet.do?id=%s", item.id);
		String remoteRegistrationUrl = Configuration
				.get("remoteRegistrationUrl");
		if (Ax.notBlank(remoteRegistrationUrl)) {
			try {
				registerRemote(Io.read().path(item.tmpFileName).asBytes(),
						item.fileName, item.mimeType, item.id);
				Ax.out("Registered remote download %s at %s", item.id,
						remoteRegistrationUrl);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return item.id;
	}

	public static String registerRemote(byte[] bytes, String fileName,
			String mimeType, String id) throws Exception {
		DownloadRegistration registration = new DownloadRegistration(bytes,
				fileName, mimeType, id);
		SimpleHttp http = new SimpleHttp(
				Configuration.get("remoteRegistrationUrl"))
						.withPostBodyQueryParameters(StringMap.properties(
								REGISTER_DOWNLOAD, String.valueOf(true),
								PAYLOAD,
								ReflectiveSerializer.serialize(registration)));
		return http.asString();
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse res)
			throws ServletException {
		doPost(request, res);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException {
		String registerDownload = request.getParameter(REGISTER_DOWNLOAD);
		if (Objects.equals(registerDownload, "true")) {
			try {
				handleAdd(request, response);
			} catch (Exception e) {
				throw new ServletException(e);
			}
		} else {
			handleDownload(request, response);
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

	private void handleAdd(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String payload = request.getParameter(PAYLOAD);
		DownloadRegistration registration = ReflectiveSerializer
				.deserialize(payload);
		add(registration.asItem());
	}

	private void handleDownload(HttpServletRequest request,
			HttpServletResponse response) {
		String id = request.getParameter("id");
		if (!items.containsKey(id)) {
			System.out.println("Download request not served: " + id);
			response.setStatus(404);
			return;
		}
		System.out.println("Download request served: " + id);
		DownloadItem item = items.get(id);
		final File f = new File(item.tmpFileName);
		if (request.getParameter("noheaders") == null) {
			response.setContentType(item.mimeType);
			if (f.length() < Integer.MAX_VALUE / 2) {
				response.setContentLength((int) f.length());
			}
			if (item.fileName != null) {
				String fixedFileName = item.fileName
						.replaceAll("[^\\x20-\\x7e]", "");
				response.setHeader("Content-Disposition",
						"attachment; filename=\"" + fixedFileName + "\"");
			}
		}
		try {
			Io.Streams.copy(new BufferedInputStream(new FileInputStream(f)),
					response.getOutputStream());
			TimerService.get().schedule(() -> {
				items.remove(id);
				f.delete();
			}, TimeConstants.ONE_MINUTE_MS);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class DownloadItem {
		private final String mimeType;

		private final String fileName;

		private final String tmpFileName;

		private String id;

		public DownloadItem(String mimeType, String fileName,
				String tmpFileName) {
			this(mimeType, fileName, tmpFileName, SEUtilities.generateId());
		}

		public DownloadItem(String mimeType, String fileName,
				String tmpFileName, String id) {
			this.mimeType = mimeType;
			this.fileName = fileName;
			this.tmpFileName = tmpFileName;
			this.id = id;
		}
	}

	public static class DownloadRegistration implements ReflectiveSerializable {
		private byte[] bytes;

		private String fileName;

		private String mimeType;

		private String id;

		public DownloadRegistration() {
		}

		public DownloadRegistration(byte[] bytes, String fileName,
				String mimeType, String id) {
			this.bytes = bytes;
			this.fileName = fileName;
			this.mimeType = mimeType;
			this.id = id;
		}

		public DownloadItem asItem() {
			try {
				File tempFile = File.createTempFile("DownloadItem", ".dat");
				Io.write().bytes(bytes).toFile(tempFile);
				return new DownloadItem(mimeType, fileName, tempFile.getPath(),
						id);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		public byte[] getBytes() {
			return this.bytes;
		}

		public String getFileName() {
			return this.fileName;
		}

		public String getId() {
			return this.id;
		}

		public String getMimeType() {
			return this.mimeType;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}
	}
}

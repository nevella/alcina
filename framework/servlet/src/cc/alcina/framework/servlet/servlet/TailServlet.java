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

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;

/**
 * <p>
 * <b>2022-05-27</b> TailServlet supports a regex filter as a querystring
 * parameter, so /tail?foo will only emit lines containing 'foo'
 * </p>
 *
 * @author Nick Reddel
 */
public class TailServlet extends AlcinaServlet {
	private boolean finished;

	private byte[] bytes(String string) {
		return string.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public void destroy() {
		finished = true;
		super.destroy();
	}

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (!isPermitted()) {
			throw new RuntimeException("Access not permitted");
		}
		File logFile = new File(Configuration.get("file"));
		String message = Ax.format("Starting tail servlet - %s", logFile);
		String filterString = request.getQueryString();
		Pattern filter = Ax.isBlank(filterString) ? null
				: Pattern.compile(filterString);
		try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
			raf.seek(raf.length());
			response.setContentType("text/html");
			response.getOutputStream().write(bytes(Ax.format(
					"<html><head><style>body{white-space: pre; font-family:monospace;}</style></head><body>%s<br><hr><br>\n",
					message)));
			response.getOutputStream().flush();
			while (!finished) {
				try {
					int length = (int) (raf.length() - raf.getFilePointer());
					if (length > 0) {
						byte[] buf = new byte[length];
						raf.readFully(buf);
						String s = new String(buf, StandardCharsets.UTF_8);
						String[] lines = s.split("\n");
						for (String line : lines) {
							if (filter == null || filter.matcher(line).find()) {
								response.getOutputStream()
										.write((line + "\n").getBytes());
							}
						}
						response.getOutputStream().flush();
					}
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
			response.getOutputStream()
					.write(bytes("<hr>Servlet destroyed<hr>"));
			response.getOutputStream().close();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected boolean isPermitted() {
		return PermissionsManager.get().isAdmin();
	}

	/*
	 * Use behind a 'secret' url -- /tail23090w94eu0293 say -- and use sparingly
	 */
	public static class NonAuthenticated extends TailServlet {
		@Override
		protected boolean isPermitted() {
			return Configuration.is(TailServlet.class,
					"permitNonAuthenticated");
		}
	}
}

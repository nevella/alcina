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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;

/**
 *
 * @author Nick Reddel
 */
public class TailServlet extends AlcinaServlet {
	private boolean finished;

	@Override
	public void destroy() {
		finished = true;
		super.destroy();
	}

	private byte[] bytes(String string) {
		return string.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (!PermissionsManager.get().isAdmin()) {
			throw new RuntimeException("Access not permitted");
		}
		File logFile = new File(ResourceUtilities.get(getClass(), "file"));
		String message = Ax.format("Starting tail servlet - %s", logFile);
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
						response.getOutputStream().write(buf);
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
}

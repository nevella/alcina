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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;


/**
 *
 * @author Nick Reddel
 */

 public abstract class FsServlet extends HttpServlet {
	private static final int WRITE_PACKET_SIZE = 100000;

	protected Logger logger;

	public FsServlet() {
	}

	protected abstract String getBasePath(ServletConfig servletConfig);
	protected String getFileRequestPath(HttpServletRequest request, HttpServletResponse response) throws IOException{
		String id = request.getParameter("id");
		if (id == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
		String path = getBasePath(getServletConfig()) + "/" + id;
		return path;
	}
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			
			String path = getFileRequestPath(request, response);
			if (path==null){
				return;
			}
			File resource = new File(path);
			if (!resource.exists() || !resource.canRead()
					|| resource.isDirectory()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			// does the client already have this file? if so, then 304
			Date ifModDate = new Date(request
					.getDateHeader("If-Modified-Since"));
			Date lastMod = new Date(resource.lastModified());
			if (lastMod.compareTo(ifModDate) <= 0) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
			// looks like we'll be serving up the file ... lets set some headers
			// set last-modified date so we can do if-modified-since checks
			// set the content type based on whatever is in our web.xml mime
			// defs
			response.addDateHeader("Last-Modified", (new Date()).getTime());
			response.setContentType(this.getServletContext().getMimeType(
					resource.getAbsolutePath()));
			response.setContentLength((int) resource.length());
			// ok, lets serve up the file
			byte[] buf = new byte[WRITE_PACKET_SIZE];
			int length = 0;
			OutputStream out = response.getOutputStream();
			InputStream resourceFile = new FileInputStream(resource);
			while ((length = resourceFile.read(buf)) > 0)
				out.write(buf, 0, length);
			// cleanup
			out.close();
			resourceFile.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}
}

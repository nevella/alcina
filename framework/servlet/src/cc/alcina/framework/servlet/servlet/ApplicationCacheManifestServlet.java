/*
 * Copyright 2009 Bart Guijt and others.
 * 
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves the cache-manifest resource with the <code>text/cache-manifest</code>
 * MIME type.
 * 
 * @author bguijt
 * 
 *         Moved to Alcina by Nick - tx bguijt
 */
public class ApplicationCacheManifestServlet extends HttpServlet {
	private static final long serialVersionUID = 6970120146736639472L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/cache-manifest");
		PrintWriter out = resp.getWriter();
		out.print(getCacheManifest(req));
		out.flush();
		resp.flushBuffer();
		log("Hitting a cache manifest: " + req.getRequestURI());
	}

	@Override
	public void log(String msg) {
		// super.log(msg);
	}

	protected String getCacheManifest(HttpServletRequest req)
			throws ServletException {
		String cacheManifest = null;
		String cacheManifestName = req.getRequestURI();
		try {
			log("Loading HTML5 cache-manifest '" + cacheManifestName
					+ "' upfront...");
			BufferedReader reader = new BufferedReader(new FileReader(
					getServletContext().getRealPath(cacheManifestName)));
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
			reader.close();
			cacheManifest = sb.toString();
			log("HTML5 cache-manifest '" + cacheManifestName
					+ "' loaded successfully.");
		} catch (FileNotFoundException e) {
			throw new ServletException("cache-manifest named '"
					+ cacheManifestName + "' NOT found!", e);
		} catch (IOException e) {
			throw new ServletException(
					"Exception while reading cache-manifest named '"
							+ cacheManifestName + "'", e);
		}
		return cacheManifest;
	}
}

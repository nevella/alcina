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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.entity.ResourceUtilities;

/**
 * Serves the cache-manifest resource with the <code>text/cache-manifest</code>
 * MIME type.
 * 
 * @author bguijt
 * 
 *         Moved to Alcina by Nick - tx bguijt
 */
public class CrxServlet extends HttpServlet {
	private static final long serialVersionUID = 6970120146736639472L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("application/x-chrome-extension");
		ServletOutputStream out = resp.getOutputStream();
		String realPath = getServletContext().getRealPath(req.getRequestURI());
		ResourceUtilities.writeStreamToStream(new BufferedInputStream(
				new FileInputStream(realPath)), out);
		resp.flushBuffer();
	}
}

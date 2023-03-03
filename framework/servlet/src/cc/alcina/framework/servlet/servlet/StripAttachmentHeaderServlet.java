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

import java.io.ByteArrayInputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SimpleHttp;

/**
 *
 * @author Nick Reddel
 */
public class StripAttachmentHeaderServlet extends HttpServlet {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException {
		String url = request.getRequestURI().substring("/direct/".length());
		if (!url.matches("https://drive.google.com.*")) {
			throw new ServletException("Invalid url");
		}
		String sUrl = url + "?" + request.getQueryString();
		try {
			ServletOutputStream os = response.getOutputStream();
			SimpleHttp query = new SimpleHttp(sUrl);
			byte[] bytes = query.asBytes();
			response.setContentType(query.getContentType());
			response.setContentLength(bytes.length);
			Io.Streams.copy(new ByteArrayInputStream(bytes), os);
		} catch (Exception e) {
			String message = String.format("Problem requesting url: \n%s\n",
					url);
			System.out.println(message);
			e.printStackTrace();
			throw new ServletException(message);
		}
	}
}

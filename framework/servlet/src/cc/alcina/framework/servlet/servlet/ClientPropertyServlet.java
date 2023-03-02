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

import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.logic.ClientProperties;
import cc.alcina.framework.servlet.CookieUtils;

/**
 *
 * @author Nick Reddel
 */
public class ClientPropertyServlet extends HttpServlet {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	public ClientPropertyServlet() {
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String[] parts = request.getPathInfo().substring(1).split("/");
		String cookieName = ClientProperties.class.getName();
		String existing = CookieUtils.getCookieValueByName(request, cookieName);
		StringMap map = new StringMap();
		try {
			if (Ax.notBlank(existing)) {
				map = StringMap.fromPropertyString(
						UrlComponentEncoder.get().decode(existing));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		String key = Ax.format("%s.%s", parts[0], parts[1]);
		map.put(key, parts[2]);
		Cookie cookie = new Cookie(cookieName,
				UrlComponentEncoder.get().encode(map.toPropertyString()));
		cookie.setPath("/");
		cookie.setMaxAge(86400 * 365 * 10);
		cookie.setSecure(Configuration.is(HttpContext.class, "secure"));
		CookieUtils.addToRequestAndResponse(request, response, cookie);
		String message = Ax.format("Map :: =>\n%s", map.entrySet().stream()
				.map(Object::toString).collect(Collectors.joining("\n")));
		logger.info(message);
		response.setContentType("text/plain");
		response.getWriter().write(message);
	}
}

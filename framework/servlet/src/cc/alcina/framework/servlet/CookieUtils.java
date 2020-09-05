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
package cc.alcina.framework.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Nick Reddel
 */
public class CookieUtils {
	public static final String REMEMBER_ME = "rememberme";

	public static final String ADDED_COOKIES_ATTR = CookieUtils.class.getName()
			+ "_addedcookies";

	public void clearRemembermeCookie(HttpServletRequest request,
			HttpServletResponse response) {
		throw new UnsupportedOperationException();
	}

	private static List<Cookie> getAddedCookies(HttpServletRequest request) {
		List<Cookie> addedCookies = (List<Cookie>) request
				.getAttribute(ADDED_COOKIES_ATTR);
		if (addedCookies == null) {
			addedCookies = new ArrayList<Cookie>();
			request.setAttribute(ADDED_COOKIES_ATTR, addedCookies);
		}
		return addedCookies;
	}

	public static String getCookieValueByName(HttpServletRequest request,
			String name) {
		// Try added cookies first
		List<Cookie> addedCookies = getAddedCookies(request);
		for (Cookie cookie : addedCookies) {
			if (cookie.getName().equals(name)) {
				return cookie.getValue();
			}
		}
		// Then try original cookies
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public static void addToRequestAndResponse(HttpServletRequest request,
			HttpServletResponse response, Cookie cookie) {
		getAddedCookies(request).add(cookie);
		response.addCookie(cookie);
	}
}

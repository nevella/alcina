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

import cc.alcina.framework.common.client.entity.Iid;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;


/**
 *
 * @author Nick Reddel
 */

 public class CookieHelper {
	public static final String REMEMBER_ME = "rememberme";

	public static final String ADDED_COOKIES_ATTR = CookieHelper.class
			.getName()
			+ "_addedcookies";

	public static final String IID = "IID";

	private CookieHelper() {
		super();
	}

	private static CookieHelper theInstance;

	public static CookieHelper get() {
		if (theInstance == null) {
			theInstance = new CookieHelper();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}
	@SuppressWarnings("unchecked")
	List<Cookie> getAddedCookies(HttpServletRequest req) {
		List<Cookie> addedCookies = (List<Cookie>) req
				.getAttribute(ADDED_COOKIES_ATTR);
		if (addedCookies == null) {
			addedCookies = new ArrayList<Cookie>();
		}
		return addedCookies;
	}

	String getCookieValueByName(HttpServletRequest req,
			HttpServletResponse response, String n) {
		Cookie[] cookies = req.getCookies();
		if (cookies == null) {
			cookies = getAddedCookies(req).toArray(new Cookie[0]);
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(n)) {
				return cookie.getValue();
			}
		}
		return null;
	}
	public String getCookieValueByName(HttpServletRequest req,
			String n) {
		Cookie[] cookies = req.getCookies();
		if (cookies==null){
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(n)) {
				return cookie.getValue();
			}
		}
		return null;
	}
	public String getIid(HttpServletRequest request,
			HttpServletResponse response) {
		String iid = getCookieValueByName(request, response, IID);
		if (iid == null) {
			iid = SEUtilities.generateId();
			Cookie cookie = new Cookie(IID, iid);
			cookie.setMaxAge(86400*365*10);
			addToRqAndRsp(request, response, cookie);
			CommonPersistenceLocal up = ServletLayerLocator.get()
					.commonPersistenceProvider().getCommonPersistence();
			up.updateIid(iid, null, false);
		}
		return iid;
	}

	void addToRqAndRsp(HttpServletRequest request,
			HttpServletResponse response, Cookie cookie) {
		getAddedCookies(request).add(cookie);
		response.addCookie(cookie);
	}

	public void setRememberMeCookie(HttpServletRequest request,
			HttpServletResponse response, boolean rememberMe) {
		if (rememberMe) {
			CommonPersistenceLocal up = ServletLayerLocator.get()
					.commonPersistenceProvider().getCommonPersistence();
			up.updateIid(getIid(request, response), PermissionsManager.get()
					.getUserName(), rememberMe);
		}
		Cookie cookie = new Cookie(REMEMBER_ME, String.valueOf(rememberMe));
		cookie.setMaxAge(86400*365*10);
		addToRqAndRsp(request,response,cookie);
	}

	public String getRememberedUserName(HttpServletRequest request,
			HttpServletResponse response) {
		String rem = getCookieValueByName(request, response, REMEMBER_ME);
		boolean b = Boolean.valueOf(rem);
		if (b) {
			CommonPersistenceLocal up = ServletLayerLocator.get()
					.commonPersistenceProvider().getCommonPersistence();
			
			return up.getRememberMeUserName(getIid(request,null));
		}
		return null;
	}

	public void clearRemembermeCookie(HttpServletRequest request,
			HttpServletResponse response) {
		CommonPersistenceLocal up = ServletLayerLocator.get()
				.commonPersistenceProvider().getCommonPersistence();
		up.updateIid(getIid(request, response), PermissionsManager.get()
				.getUserName(), false);
	}
}

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;

/**
 * 
 * @author Nick Reddel
 */
public class SessionHelper {
	public static final String SESSION_ATTR_USERNAME = "SESSION_ATTR_USERNAME";

	public static final String REQUEST_ATTR_INITIALISED = "REQUEST_ATTR_INITIALISED";

	public static final String SESSION_ATTR_ONE_TIME_STRING = "SESSION_ATTR_ONE_TIME_STRING";

	private static void initaliseRequest(HttpServletRequest request) {
		if (request.getAttribute(REQUEST_ATTR_INITIALISED) == null) {
			HttpSession session = request.getSession();
			synchronized (session) {
				request.setAttribute(SESSION_ATTR_USERNAME, session
						.getAttribute(SESSION_ATTR_USERNAME));
				request.setAttribute(REQUEST_ATTR_INITIALISED, true);
			}
		}
	}

	public static void initUserState(HttpServletRequest request) {
		initaliseRequest(request);
		String clientInstanceId = request
				.getHeader(AlcinaRpcRequestBuilder.CLIENT_INSTANCE_ID_KEY);
		if (clientInstanceId != null) {
			String clientInstanceAuth = request
					.getHeader(AlcinaRpcRequestBuilder.CLIENT_INSTANCE_AUTH_KEY);
			CommonPersistenceLocal up = ServletLayerLocator.get()
					.commonPersistenceProvider().getCommonPersistence();
			String userName = up.validateClientInstance(Long
					.parseLong(clientInstanceId), Integer
					.parseInt(clientInstanceAuth));
			if (userName!=null){
				request.getSession().setAttribute(SESSION_ATTR_USERNAME,
						userName);
				request.setAttribute(SESSION_ATTR_USERNAME, userName);
			}
		}
		reinitialiseUserState(request);
	}

	public static void invalidateSession(HttpServletRequest rq) {
		rq.getSession().invalidate();
	}

	public static void resetSession(HttpServletRequest request) {
		request.setAttribute(SESSION_ATTR_USERNAME, null);
		request.getSession().setAttribute(SESSION_ATTR_USERNAME, null);
	}

	public static void setupSessionForUser(HttpServletRequest request,
			IUser user) {
		request.getSession().setAttribute(SESSION_ATTR_USERNAME,
				user.getUserName());
		request.setAttribute(SESSION_ATTR_USERNAME, user.getUserName());
		PermissionsManager.get().setLoginState(LoginState.LOGGED_IN);
		PermissionsManager.get().setUser(user);
	}

	private static void resetPermissions() {
		PermissionsManager.get().setLoginState(LoginState.NOT_LOGGED_IN);
		CommonPersistenceLocal up = ServletLayerLocator.get()
				.commonPersistenceProvider().getCommonPersistence();
		PermissionsManager.get().setUser(up.getSystemUser(true));
	}

	public static void reinitialiseUserState(HttpServletRequest request) {
		resetPermissions();
		String userName = (String) request.getAttribute(SESSION_ATTR_USERNAME);
		if (userName != null) {
			CommonPersistenceLocal up = ServletLayerLocator.get()
					.commonPersistenceProvider().getCommonPersistence();
			IUser user = up.getUserByName(userName, true);
			if (user != null) {
				setupSessionForUser(request, user);
			}
		}
	}
}

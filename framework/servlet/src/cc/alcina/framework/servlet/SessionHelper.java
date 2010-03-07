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

import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;


/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class SessionHelper {
	public static final String SESSION_ATTR_USERNAME = "username";
	public static final String SESSION_ATTR_ONE_TIME_STRING= "SESSION_ATTR_ONE_TIME_STRING";

	public static void initUserState(HttpServletRequest rq) {
		resetPermissions();
		String userName = (String) rq.getSession().getAttribute(
				SESSION_ATTR_USERNAME);
		if (userName != null) {
			CommonPersistenceLocal up = ServerLayerLocator.get().commonPersistenceProvider()
					.getCommonPersistence();
			IUser user = up.getUserByName(userName,true);
			if (user!=null){
				setupSessionForUser(rq, user);
			}
		}
	}

	public static void setupSessionForUser(HttpServletRequest rq, IUser user) {
		rq.getSession().setAttribute(SESSION_ATTR_USERNAME, user.getUserName());
		PermissionsManager.get().setLoginState(LoginState.LOGGED_IN);
		PermissionsManager.get().setUser(user);
	}

	static void resetPermissions() {
		PermissionsManager.get().setLoginState(LoginState.NOT_LOGGED_IN);
		CommonPersistenceLocal up = ServerLayerLocator.get().commonPersistenceProvider().getCommonPersistence();
		PermissionsManager.get().setUser(up.getSystemUser(true));
	}

	public static void resetSession(HttpServletRequest rq) {
		rq.getSession().setAttribute(SESSION_ATTR_USERNAME, null);
	}
	public static void invalidateSession(HttpServletRequest rq) {
		rq.getSession().invalidate();
	}
}

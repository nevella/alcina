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

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.logic.permissions.ThreadedPmClientInstanceResolver;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.ServletLayerTransforms;

/**
 * 
 * @author Nick Reddel
 */
@RegistryLocation(registryPoint = SessionHelper.class, implementationType = ImplementationType.SINGLETON)
public class SessionHelper {
    public static final String SESSION_ATTR_USERNAME = "SESSION_ATTR_USERNAME";

    public static final String SESSION_AUTHENTICATED_CLIENT_INSTANCE_ID = "SESSION_AUTHENTICATED_CLIENT_INSTANCE_ID";

    public static final String REQUEST_ATTR_INITIALISED = "REQUEST_ATTR_INITIALISED";

    public static final String SESSION_ATTR_ONE_TIME_STRING = "SESSION_ATTR_ONE_TIME_STRING";

    public static ClientInstance getAuthenticatedSessionClientInstance(
            HttpServletRequest request) {
        Long clientInstanceId = getAuthenticatedSessionClientInstanceId(
                request);
        if (clientInstanceId != null) {
            return CommonPersistenceProvider.get().getCommonPersistence()
                    .getClientInstance(clientInstanceId);
        } else {
            return null;
        }
    }

    public static Long getAuthenticatedSessionClientInstanceId(
            HttpServletRequest request) {
        if (request == null) {
            return null;
        } else {
            return (Long) request
                    .getAttribute(SESSION_AUTHENTICATED_CLIENT_INSTANCE_ID);
        }
    }

    public Long getAuthenticatedClientInstanceId(HttpServletRequest request) {
        return (Long) request
                .getAttribute(SESSION_AUTHENTICATED_CLIENT_INSTANCE_ID);
    }

    public String getClientInstanceId(HttpServletRequest request) {
        String clientInstanceId = request
                .getHeader(AlcinaRpcRequestBuilder.CLIENT_INSTANCE_ID_KEY);
        return clientInstanceId;
    }

    public String getValidatedClientInstanceUserName(long clientInstanceId,
            int clientInstanceAuth) {
        CommonPersistenceLocal up = Registry
                .impl(CommonPersistenceProvider.class).getCommonPersistence();
        if (up.validateClientInstance(clientInstanceId, clientInstanceAuth)) {
            return up.getUserNameForClientInstanceId(clientInstanceId);
        }
        return null;
    }

    public void initUserState(HttpServletRequest request,
            HttpServletResponse response) {
        initaliseRequest(request, response);
        String clientInstanceId = getClientInstanceId(request);
        if (clientInstanceId != null) {
            String clientInstanceAuth = request.getHeader(
                    AlcinaRpcRequestBuilder.CLIENT_INSTANCE_AUTH_KEY);
            try {
                String userName = getValidatedClientInstanceUserName(
                        Long.parseLong(clientInstanceId),
                        Integer.parseInt(clientInstanceAuth));
                if (userName != null) {
                    getSession(request, response)
                            .setAttribute(SESSION_ATTR_USERNAME, userName);
                    request.setAttribute(SESSION_ATTR_USERNAME, userName);
                    request.setAttribute(
                            SESSION_AUTHENTICATED_CLIENT_INSTANCE_ID,
                            Long.valueOf(clientInstanceId));
                }
            } catch (NumberFormatException nfe) {
                // squelch
            }
        }
        reinitialiseUserState(request, response);
    }

    public void invalidateSession(HttpServletRequest request,
            HttpServletResponse response) {
        getSession(request, response).invalidate();
    }

    public void reinitialiseUserState(HttpServletRequest request,
            HttpServletResponse response) {
        resetPermissions(request);
        String userName = (String) request.getAttribute(SESSION_ATTR_USERNAME);
        if (userName != null) {
            IUser user = getUser(userName);
            if (user != null) {
                setupSessionForUser(request, response, user);
            }
        }
    }

    public void resetSession(HttpServletRequest request,
            HttpServletResponse response) {
        request.setAttribute(SESSION_ATTR_USERNAME, null);
        getSession(request, response).setAttribute(SESSION_ATTR_USERNAME, null);
    }

    public void setupSessionForUser(HttpServletRequest request,
            HttpServletResponse response, IUser user) {
        if (request != null) {
            getSession(request, response).setAttribute(SESSION_ATTR_USERNAME,
                    user.getUserName());
            request.setAttribute(SESSION_ATTR_USERNAME, user.getUserName());
        }
        PermissionsManager.get().setUser(user);
        PermissionsManager.get().setAuthenticatedClientInstanceId(
                getAuthenticatedSessionClientInstanceId(request));
        if (!isAnonymousUser()) {
            PermissionsManager.get().setLoginState(LoginState.LOGGED_IN);
        }
    }

    private HttpSession getSession(HttpServletRequest request,
            HttpServletResponse resp) {
        return Registry.impl(SessionProvider.class).getSession(request, resp);
    }

    private void initaliseRequest(HttpServletRequest request,
            HttpServletResponse resp) {
        if (request.getAttribute(REQUEST_ATTR_INITIALISED) == null) {
            HttpSession session = getSession(request, resp);
            synchronized (session) {
                request.setAttribute(SESSION_ATTR_USERNAME,
                        session.getAttribute(SESSION_ATTR_USERNAME));
                request.setAttribute(REQUEST_ATTR_INITIALISED, true);
            }
        }
    }

    protected IUser getUser(String userName) {
        CommonPersistenceLocal up = Registry
                .impl(CommonPersistenceProvider.class).getCommonPersistence();
        return up.getUserByName(userName, true);
    }

    protected boolean isAnonymousUser() {
        return PermissionsManager.get().isAnonymousUser();
    }

    protected void resetPermissions(HttpServletRequest request) {
        ThreadedPermissionsManager.cast().reset();
        PermissionsManager.get().setLoginState(LoginState.NOT_LOGGED_IN);
        CommonPersistenceLocal up = Registry
                .impl(CommonPersistenceProvider.class)
                .getCommonPersistenceExTransaction();
        PermissionsManager.get().setUser(getUser(up.getAnonymousUserName()));
    }

    public static class ThreadedPmClientInstanceResolverImpl
            extends ThreadedPmClientInstanceResolver {
        @Override
        public ClientInstance getClientInstance() {
            HttpServletRequest request = CommonRemoteServiceServlet
                    .getContextThreadLocalRequest();
            ClientInstance result = null;
            if (request != null) {
                Long clientInstanceId = getAuthenticatedSessionClientInstanceId(
                        request);
                if (clientInstanceId != null) {
                    result = Registry.impl(CommonPersistenceProvider.class)
                            .getCommonPersistence()
                            .getClientInstance(clientInstanceId);
                }
            }
            return Optional.<ClientInstance> ofNullable(result)
                    .orElse(Registry.impl(ServletLayerTransforms.class)
                            .getServerAsClientInstance());
        }
    }
}

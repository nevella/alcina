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

import java.io.OutputStream;
import java.lang.reflect.Method;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.logic.permissions.AnnotatedPermissible;
import cc.alcina.framework.common.client.logic.permissions.AuthenticationRequired;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.servlet.CookieHelper;
import cc.alcina.framework.servlet.SessionHelper;
import cc.alcina.framework.servlet.authentication.AuthenticationException;

import com.google.gwt.rpc.server.ClientOracle;
import com.google.gwt.rpc.server.RpcServlet;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.impl.LegacySerializationPolicy;

/**
 * Tests (todo) for transform persistence: invalid clientauth multiple
 * simultaneous (identical clientinstance, non-) cross-server-restart
 * 
 * @author nick@alcina.cc
 * 
 */
@SuppressWarnings("unchecked")
public abstract class CommonRpcServlet extends RpcServlet {
	protected final CommonRemoteServiceServlet remoteServiceImpl;

	public CommonRpcServlet(CommonRemoteServiceServlet remoteServiceImpl) {
		super(remoteServiceImpl);
		this.remoteServiceImpl = remoteServiceImpl;
		
	}


	@Override
	protected void onAfterRequestDeserialized(RPCRequest rpcRequest) {
		CookieHelper.get().getIid(getThreadLocalRequest(),
				getThreadLocalResponse());
		SessionHelper.initUserState(getThreadLocalRequest());
		String userName = CookieHelper.get().getRememberedUserName(
				getThreadLocalRequest(), getThreadLocalResponse());
		if (userName != null && !PermissionsManager.get().isLoggedIn()) {
			try {
				LoginResponse lrb = new LoginResponse();
				lrb.setOk(true);
				remoteServiceImpl.processValidLogin(lrb, userName);
			} catch (AuthenticationException e) {
				// ignore
			}
		}
		if (rpcRequest.getSerializationPolicy() instanceof LegacySerializationPolicy) {
			throw new IncompatibleRemoteServiceException();
		}
		getThreadLocalRequest().setAttribute(
				CommonRemoteServiceServlet.THRD_LOCAL_RPC_RQ, rpcRequest);
		String name = rpcRequest.getMethod().getName();
		try {
			Method method;
			method = this.getClass().getMethod(name,
					rpcRequest.getMethod().getParameterTypes());
			if (method.isAnnotationPresent(AuthenticationRequired.class)) {
				AuthenticationRequired ar = method
						.getAnnotation(AuthenticationRequired.class);
				AnnotatedPermissible ap = new AnnotatedPermissible(
						ar.permission());
				if (!PermissionsManager.get().isPermissible(ap)) {
					getServletContext().log("Action not permitted: " + name,
							new Exception());
					throw new WebException("Action not permitted: " + name);
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public void processCall(ClientOracle clientOracle, String payload,
			OutputStream stream) throws SerializationException {
		try {
			super.processCall(clientOracle, payload, stream);
		} finally {
			ThreadlocalTransformManager.cast().resetTltm(null);
		}
	}
}

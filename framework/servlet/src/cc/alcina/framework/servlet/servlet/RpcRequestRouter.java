package cc.alcina.framework.servlet.servlet;

import java.lang.reflect.Method;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;

import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.ServerControlAction;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationProxy;

@RegistryLocation(registryPoint = RpcRequestRouter.class, implementationType = ImplementationType.SINGLETON)
public class RpcRequestRouter {
	public static final String CONTEXT_IN_RPC_CALL = RpcRequestRouter.class
			.getName() + ".CONTEXT_IN_RPC_CALL";

	public static RpcRequestRouter get() {
		return Registry.impl(RpcRequestRouter.class);
	}

	public String invokeAndEncodeResponse(Object target, RPCRequest rpcRequest)
			throws SerializationException {
		String requestHandlerUrl = ResourceUtilities.get("requestHandlerUrl");
		if (Ax.notBlank(requestHandlerUrl) && !forceServerHandler(rpcRequest)) {
			try {
				LooseContext.pushWithTrue(CONTEXT_IN_RPC_CALL);
				Object proxy = wrapProxy(target, rpcRequest);
				return RPC.invokeAndEncodeResponse(proxy,
						rpcRequest.getMethod(), rpcRequest.getParameters(),
						rpcRequest.getSerializationPolicy());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				LooseContext.pop();
			}
		}
		return RPC.invokeAndEncodeResponse(target, rpcRequest.getMethod(),
				rpcRequest.getParameters(),
				rpcRequest.getSerializationPolicy());
	}

	private Object wrapProxy(Object invocationTarget, RPCRequest rpcRequest) {
		for (Class clazz : invocationTarget.getClass().getInterfaces()) {
			if (RemoteService.class.isAssignableFrom(clazz) && rpcRequest
					.getMethod().getDeclaringClass().isAssignableFrom(clazz)) {
				Object[] args = rpcRequest.getParameters();
				String methodName = rpcRequest.getMethod().getName();
				Method method = new SEUtilities.MethodFinder().findMethod(clazz,
						args, methodName);
				if (method != null) {
					RemoteInvocationProxy proxy = new RemoteInvocationProxy();
					proxy.setRemoteAddress(
							ResourceUtilities.get("requestHandlerUrl"));
					return proxy.createProxy(clazz);
				}
			}
		}
		throw new UnsupportedOperationException();
	}

	protected boolean forceServerHandler(RPCRequest rpcRequest) {
		if (rpcRequest.getMethod().getName()
				.matches("performAction|performAdminAction")) {
			RemoteAction action = (RemoteAction) rpcRequest.getParameters()[0];
			if (action.getClass() == ServerControlAction.class) {
				return true;
			}
		}
		/*
		 * For the moment, avoid requests which modify the cookie state
		 */
		if (rpcRequest.getMethod().getName().matches("hello|login|logout")) {
			return true;
		}
		/*
		 * For the moment, avoid requests which modify the cookie state
		 * (reflective rpc)
		 */
		if (rpcRequest.getMethod().getName().matches("callRpc")
				&& rpcRequest.getParameters()[0].toString()
						.contains("\"methodName\":\"login\"")) {
			return true;
		}
		/*
		 * Just because the from-server response would include ClassRefImpl
		 */
		if (rpcRequest.getMethod().getName().matches("transform")) {
			return true;
		}
		return false;
	}
}

package cc.alcina.framework.servlet.servlet.remote;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.google.gwt.user.client.rpc.RemoteService;

import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationParameters.Api;

public abstract class ProxyRemoteService<I extends RemoteService>
		implements InvocationHandler {
	public I createProxy() {
		Object proxy = Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class[] { getInterfaceClass() }, this);
		return (I) proxy;
	}

	public abstract Class<I> getInterfaceClass();

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		RemoteInvocation remoter = new RemoteInvocation();
		if (remoter.tryInterception(proxy, method, args)) {
			return remoter.getInterceptionResult();
		}
		RemoteInvocationParameters params = new RemoteInvocationParameters();
		params.api = Api.GWT_REMOTE_SERVICE_IMPL;
		return new RemoteInvocation().invoke(method.getName(), args, params);
	}
}

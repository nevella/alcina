package cc.alcina.extras.dev.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cc.alcina.framework.servlet.servlet.dev.DevRemoterParams;
import cc.alcina.framework.servlet.servlet.dev.DevRemoterParams.DevRemoterApi;

import com.google.gwt.user.client.rpc.RemoteService;

public abstract class ProxyRemoteService<I extends RemoteService> implements
		InvocationHandler {
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		DevRemoter remoter = new DevRemoter();
		if (remoter.tryInterception(proxy, method, args)) {
			return remoter.getInterceptionResult();
		}
		DevRemoterParams params = new DevRemoterParams();
		params.api = DevRemoterApi.GWT_REMOTE_SERVICE_IMPL;
		return new DevRemoter().invoke(method.getName(), args, params);
	}

	public I createProxy() {
		Object proxy = Proxy.newProxyInstance(Thread.currentThread()
				.getContextClassLoader(), new Class[] { getInterfaceClass() }, this);
		return (I) proxy;
	}

	public abstract Class<I> getInterfaceClass();
}

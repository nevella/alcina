package cc.alcina.framework.servlet.servlet.remote;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.google.gwt.user.client.rpc.RemoteService;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationParameters.Api;

public class RemoteInvocationProxy implements InvocationHandler {
	public <T> T createProxy(Class<T> clazz) {
		Object proxy = Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class[] { clazz }, this);
		return (T) proxy;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		RemoteInvocation remoter = Registry.impl(RemoteInvocation.class);
		if (remoter.tryInterception(proxy, method, args)) {
			Object result = remoter.getInterceptionResult();
			return result;
		}
		Class<?> clazz = proxy.getClass().getInterfaces()[0];
		RemoteInvocationParameters params = new RemoteInvocationParameters();
		params.interfaceClassName = clazz.getName();
		params.api = 
				RemoteService.class.isAssignableFrom(proxy.getClass())?Api.GWT_REMOTE_SERVICE_IMPL:
				Api.EJB_BEAN_PROVIDER;
		return remoter.invoke(method.getName(), args, params);
	}

	public interface RemoteInvocationProxyInterceptor {
		public boolean handles(Object proxy, Method method, Object[] args);

		public void hookParams(String methodName, Object[] args,
				RemoteInvocationParameters params);

		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable;
	}
}

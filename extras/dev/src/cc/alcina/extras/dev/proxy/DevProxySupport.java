package cc.alcina.extras.dev.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.servlet.servlet.dev.DevRemoterParams;
import cc.alcina.framework.servlet.servlet.dev.DevRemoterParams.DevRemoterApi;

public class DevProxySupport implements InvocationHandler {
	public <T> T createProxy(Class<T> clazz) {
		Object proxy = Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class[] { clazz }, this);
		return (T) proxy;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		DevRemoter remoter = Registry.impl(DevRemoter.class);
		if (remoter.tryInterception(proxy, method, args)) {
			Object result = remoter.getInterceptionResult();
			return result;
		}
		Class<?> clazz = proxy.getClass().getInterfaces()[0];
		DevRemoterParams params = new DevRemoterParams();
		params.interfaceClassName = clazz.getName();
		params.api = DevRemoterApi.EJB_BEAN_PROVIDER;
		return remoter.invoke(method.getName(), args, params);
	}

	public interface DevProxyInterceptor {
		public boolean handles(Object proxy, Method method, Object[] args);

		public void hookParams(String methodName, Object[] args,
				DevRemoterParams params);

		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable;
	}
}

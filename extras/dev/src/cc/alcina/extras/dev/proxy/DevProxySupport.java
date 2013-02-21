package cc.alcina.extras.dev.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cc.alcina.framework.servlet.servlet.dev.DevRemoterParams;
import cc.alcina.framework.servlet.servlet.dev.DevRemoterParams.DevRemoterApi;

public class DevProxySupport implements InvocationHandler {
	public <T> T createProxy(Class<T> clazz) {
		Object proxy = Proxy.newProxyInstance(Thread.currentThread()
				.getContextClassLoader(), new Class[] { clazz }, this);
		return (T) proxy;
	}
	public interface DevProxyInterceptor {
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable;

		public boolean handles(Object proxy, Method method, Object[] args);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		DevRemoter remoter = new DevRemoter();
		if(remoter.tryInterception(proxy, method, args)){
			Object result = remoter.getInterceptionResult();
			return result;
		}
		
		Class<?> clazz = proxy.getClass().getInterfaces()[0];
		DevRemoterParams params = new DevRemoterParams();
		params.interfaceClassName = clazz.getName();
		params.api=DevRemoterApi.EJB_BEAN_PROVIDER;
		return new DevRemoter().invoke(method.getName(), args, params);
	}
}

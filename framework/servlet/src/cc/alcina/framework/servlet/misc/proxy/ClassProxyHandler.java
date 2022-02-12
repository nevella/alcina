package cc.alcina.framework.servlet.misc.proxy;

import java.util.List;

public interface ClassProxyHandler {
	Object handle(ClassProxy proxy, String methodName,
			List<Class> methodArgTypes, List<?> methodArgs);
}

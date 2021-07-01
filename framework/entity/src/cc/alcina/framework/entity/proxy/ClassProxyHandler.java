package cc.alcina.framework.entity.proxy;

import java.util.List;

public interface ClassProxyHandler {
	Object handle(ClassProxy proxy, String methodName, List<Class> methodArgTypes, List<?> methodArgs);
}

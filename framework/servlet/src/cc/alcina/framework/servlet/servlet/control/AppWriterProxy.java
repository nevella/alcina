package cc.alcina.framework.servlet.servlet.control;

import java.lang.reflect.Method;

import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

import com.google.gwt.user.server.rpc.RPCRequest;

//not done yet, but for having a per-cluster writer machine
public class AppWriterProxy {
	public String proxy(AppLifecycleManager appLifecycleManager,
			RPCRequest rpcRequest, CommonRemoteServiceServlet remoteServlet) {
		Method method = rpcRequest.getMethod();
		return null;
	}
}

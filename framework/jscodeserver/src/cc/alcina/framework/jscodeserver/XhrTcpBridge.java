package cc.alcina.framework.jscodeserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * TODO-jscs
 * 
 * Disconnect after timeout
 * 
 * 
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = XhrTcpBridge.class, implementationType = ImplementationType.SINGLETON)
public class XhrTcpBridge {
	public static final String HEADER_HANDLE_ID = "XhrTcpBridge.handle_id";

	public static final String HEADER_MESSAGE_ID = "XhrTcpBridge.message_id";

	public static final String HEADER_CODE_SERVER_PORT = "XhrTcpBridge.codeserver_port";

	public static final String HEADER_META = "XhrTcpBridge.meta";

	public static XhrTcpBridge get() {
		return Registry.impl(XhrTcpBridge.class);
	}

	AtomicInteger sessionCounter = new AtomicInteger();

	Map<String, XhrTcpSession> sessions = new ConcurrentHashMap<>();

	public void handle(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		/*
		 * if we have no handle header, generate and add it
		 */
		String handleId = request.getHeader(HEADER_HANDLE_ID);
		XhrTcpSession session = null;
		if (handleId == null) {
			session = new XhrTcpSession(this);
			session.handleId = sessionCounter.incrementAndGet();
			sessions.put(String.valueOf(session.handleId), session);
		} else {
			session = sessions.get(handleId);
		}
		if (session == null) {
			// unknown, discard call
			return;
		}
		session.handle(request, response);
	}

	public void removeSession(XhrTcpSession xhrTcpSession) {
		// keep so we have it available for cleanup/exceptions
		// sessions.remove(String.valueOf(xhrTcpSession.handleId));
	}
}

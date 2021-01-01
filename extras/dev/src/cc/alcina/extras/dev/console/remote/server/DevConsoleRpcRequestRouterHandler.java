package cc.alcina.extras.dev.console.remote.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.extras.dev.console.DevConsole;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationParameters;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationProxy;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationServlet;

public class DevConsoleRpcRequestRouterHandler extends AbstractHandler {
	DevConsoleRemote devConsoleRemote;

	public DevConsoleRpcRequestRouterHandler(
			DevConsoleRemote devConsoleRemote) {
		this.devConsoleRemote = devConsoleRemote;
	}

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			LooseContext.pushWithTrue(
					RemoteInvocationProxy.CONTEXT_NO_LINK_TO_DOMAIN);
			Registry.impl(DevConsole.class).ensureDomainStore();
			new RemoteInvocationServlet_DevConsole().doPost(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			response.getWriter().write(CommonUtils.toSimpleExceptionMessage(e));
		} finally {
			LooseContext.pop();
		}
	}

	private static class RemoteInvocationServlet_DevConsole
			extends RemoteInvocationServlet {
		@Override
		public void doPost(HttpServletRequest req, HttpServletResponse res)
				throws ServletException, IOException {
			super.doPost(req, res);
		}

		@Override
		protected Object getInvocationTarget(RemoteInvocationParameters params)
				throws Exception {
			return Registry.impl(CommonRemoteServiceServlet.class);
		}

		@Override
		protected void maybeToNoActiveTransaction() {
			// NOOP (yes, use txs)
		}
	}
}

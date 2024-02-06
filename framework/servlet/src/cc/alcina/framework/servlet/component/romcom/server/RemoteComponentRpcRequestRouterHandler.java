package cc.alcina.framework.servlet.component.romcom.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationParameters;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationProxy;
import cc.alcina.framework.servlet.servlet.remote.RemoteInvocationServlet;

public class RemoteComponentRpcRequestRouterHandler extends AbstractHandler {
	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			LooseContext.pushWithTrue(
					RemoteInvocationProxy.CONTEXT_NO_LINK_TO_DOMAIN);
			// Registry.impl(RemoteComponent.class).ensureDomainStore();
			Transaction.ensureEnded();
			RemoteInvocationServlet_RemoteComponent impl = Registry
					.impl(RemoteInvocationServlet_RemoteComponent.class);
			impl.doPost(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			response.getWriter().write(CommonUtils.toSimpleExceptionMessage(e));
		} finally {
			LooseContext.pop();
		}
	}

	@Registration(RemoteInvocationServlet_RemoteComponent.class)
	public static class RemoteInvocationServlet_RemoteComponent
			extends RemoteInvocationServlet {
		@Override
		protected void customiseContextBeforePayloadWrite() {
			RemoteInvocationServlet_RemoteComponent_Customiser.get()
					.customiseContextBeforePayloadWrite();
		}

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
		protected void maybeToReadonlyTransaction() {
			// NOOP (yes, allow writeable txs)
		}
	}
}

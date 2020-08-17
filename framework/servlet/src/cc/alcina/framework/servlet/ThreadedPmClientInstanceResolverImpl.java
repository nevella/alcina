package cc.alcina.framework.servlet;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.permissions.ThreadedPmClientInstanceResolver;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

public class ThreadedPmClientInstanceResolverImpl
		extends ThreadedPmClientInstanceResolver {
	public static final String CONTEXT_CLIENT_INSTANCE = ThreadedPmClientInstanceResolverImpl.class
			.getName() + ".CONTEXT_CLIENT_INSTANCE";

	@Override
	public ClientInstance getClientInstance() {
		if (LooseContext.has(CONTEXT_CLIENT_INSTANCE)) {
			return LooseContext.get(CONTEXT_CLIENT_INSTANCE);
		}
		Optional<ClientInstance> result = AuthenticationManager.get()
				.getContextClientInstance();
		return result
				.orElse(EntityLayerObjects.get().getServerAsClientInstance());
	}

	@Override
	public Long getClientInstanceId() {
		if (LooseContext.has(CONTEXT_CLIENT_INSTANCE)) {
			return ((ClientInstance) LooseContext.get(CONTEXT_CLIENT_INSTANCE))
					.getId();
		}
		HttpServletRequest request = CommonRemoteServiceServlet
				.getContextThreadLocalRequest();
		if (request != null) {
			Long clientInstanceId = AuthenticationManager.get()
					.getContextClientInstance().map(ClientInstance::getId)
					.orElse(null);
			if (clientInstanceId != null) {
				return clientInstanceId;
			}
		}
		ClientInstance serverAsClientInstance = EntityLayerObjects.get()
				.getServerAsClientInstance();
		return serverAsClientInstance == null ? -1
				: serverAsClientInstance.getId();
	}
}
package cc.alcina.framework.servlet;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.permissions.ThreadedPmClientInstanceResolver;
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
		HttpServletRequest request = CommonRemoteServiceServlet
				.getContextThreadLocalRequest();
		ClientInstance result = null;
		if (request != null) {
			Long clientInstanceId = SessionHelper
					.getAuthenticatedSessionClientInstanceId(request);
			if (clientInstanceId != null) {
				result = CommonPersistenceProvider.get().getCommonPersistence()
						.getClientInstance(clientInstanceId);
			}
		}
		return Optional.<ClientInstance> ofNullable(result)
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
			Long clientInstanceId = SessionHelper
					.getAuthenticatedSessionClientInstanceId(request);
			if (clientInstanceId != null) {
				return clientInstanceId;
			}
		}
		return EntityLayerObjects.get().getServerAsClientInstance().getId();
	}
}
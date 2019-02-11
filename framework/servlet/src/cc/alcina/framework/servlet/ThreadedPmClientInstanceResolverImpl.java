package cc.alcina.framework.servlet;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.logic.permissions.ThreadedPmClientInstanceResolver;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
import cc.alcina.framework.servlet.servlet.ServletLayerTransforms;

public class ThreadedPmClientInstanceResolverImpl
        extends ThreadedPmClientInstanceResolver {
    public static final String CONTEXT_CLIENT_INSTANCE = ThreadedPmClientInstanceResolverImpl.class
            .getName() + ".CONTEXT_CLIENT_INSTANCE";

    @Override
    public ClientInstance getClientInstance() {
        HttpServletRequest request = CommonRemoteServiceServlet
                .getContextThreadLocalRequest();
        ClientInstance result = null;
        if (request != null) {
            Long clientInstanceId = SessionHelper
                    .getAuthenticatedSessionClientInstanceId(request);
            if (clientInstanceId != null) {
                result = Registry.impl(CommonPersistenceProvider.class)
                        .getCommonPersistence()
                        .getClientInstance(clientInstanceId);
            }
        }
        if (LooseContext.has(CONTEXT_CLIENT_INSTANCE)) {
            return LooseContext.get(CONTEXT_CLIENT_INSTANCE);
        }
        return Optional.<ClientInstance> ofNullable(result).orElse(
                ServletLayerTransforms.get().getServerAsClientInstance());
    }
}
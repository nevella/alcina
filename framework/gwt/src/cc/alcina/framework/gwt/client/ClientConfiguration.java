package cc.alcina.framework.gwt.client;

import com.google.gwt.dom.client.StyleInjector;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.DomainHandlerClient;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContext.ClientLooseContextProvider;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.res.AlcinaProperties;
import cc.alcina.framework.gwt.client.res.AlcinaResources;
import cc.alcina.framework.gwt.client.util.TimerWrapperGwt.TimerWrapperProviderGwt;

public class ClientConfiguration {
    public void initServices() {
        initNotifications();
        initCss();
        Registry.impl(ClientNotifications.class).metricLogStart("moduleLoad");
        initExceptionHandling();
        initCommonClient();
        initContentProvider();
        prepareDebugFromHistory();
        extraConfiguration();
    }

    protected void afterConfiguration() {
    }

    protected CommitToStorageTransformListener createStorageTransformListener() {
        return new CommitToStorageTransformListener();
    }

    protected ClientTransformManager createTransformManager() {
        return new ClientTransformManagerCommon();
    }

    protected void extraConfiguration() {
    }

    protected void initCommonClient() {
        TransformManager.register(createTransformManager());
        Domain.registerHandler(new DomainHandlerClient());
        LooseContext.register(new ClientLooseContextProvider());
        TransformManager.get().setupClientListeners();
        TransformManager.get()
                .addDomainTransformListener(PermissionsManager.get());
        Registry.registerSingleton(CommitToStorageTransformListener.class,
                createStorageTransformListener());
        Registry.registerSingleton(TimerWrapperProvider.class,
                new TimerWrapperProviderGwt());
        registerExtraTransformListenersPreStorage();
        TransformManager.get().addDomainTransformListener(
                Registry.impl(CommitToStorageTransformListener.class));
        registerExtraTransformListenersPostStorage();
        Reflections.registerPropertyAccessor(GwittirBridge.get());
        Reflections.registerBeanDescriptorProvider(GwittirBridge.get());
        Reflections.registerClassLookup(ClientReflector.get());
        Reflections.registerObjectLookup(TransformManager.get());
    }

    protected void initContentProvider() {
    }

    protected void initCss() {
        StyleInjector.inject(AlcinaResources.INSTANCE.css().getText());
    }

    protected void initExceptionHandling() {
        Registry.registerSingleton(ClientExceptionHandler.class,
                new ClientExceptionHandler());
    }

    protected void initImageProvider() {
    }

    protected void initNotifications() {
        Registry.registerSingleton(ClientNotifications.class,
                new ClientNotificationsImpl());
    }

    protected void prepareDebugFromHistory() {
        AlcinaHistory.initialiseDebugIds();
        if (AlcinaProperties.is(AlcinaProperties.class,
                AlcinaProperties.SIMULATE_OFFLINE)) {
            AlcinaDebugIds.setFlag(AlcinaDebugIds.DEBUG_SIMULATE_OFFLINE);
        }
    }

    protected void registerExtraTransformListenersPostStorage() {
    }

    protected void registerExtraTransformListenersPreStorage() {
    }
}

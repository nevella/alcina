package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.util.LooseContextProvider;
import cc.alcina.framework.common.client.util.LooseContextProvider.ClientLooseContextProvider;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.ide.provider.DataImageProvider;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;
import cc.alcina.framework.gwt.client.logic.ClientUTCDateProvider;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.provider.ClientURLComponentEncoder;
import cc.alcina.framework.gwt.client.res.AlcinaResources;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImageProvider;
import cc.alcina.framework.gwt.client.util.TimerWrapperGwt.TimerWrapperProviderGwt;

import com.google.gwt.dom.client.StyleInjector;

public class ClientConfiguration {
	public void initServices() {
		initNotifications();
		initCss();
		ClientLayerLocator.get().notifications().metricLogStart("moduleLoad");
		initExceptionHandling();
		initAppCache();
		initCommonClient();
		initLocalPersistence();
	}

	protected void initServicesPostLocalPersistence() {
		initContentProvider();
		initImageProvider();
		initHandshakeHelper();
		extraConfiguration();
		prepareDebugFromHistory();
	}

	protected void prepareDebugFromHistory() {
		AlcinaDebugIds.initialise();
	}
	protected void initCss(){
		StyleInjector.inject(AlcinaResources.INSTANCE.css().getText());
	}
	protected void initAppCache() {
	}

	protected void initHandshakeHelper() {
	}

	protected void initLocalPersistence() {
		initServicesPostLocalPersistence();
	}

	protected void extraConfiguration() {
	}

	protected CommitToStorageTransformListener createStorageTransformListener() {
		return new CommitToStorageTransformListener();
	}

	protected void initImageProvider() {
	}

	protected void initContentProvider() {
	}

	protected void initCommonClient() {
		TransformManager.register(createTransformManager());
		LooseContextProvider.register(new ClientLooseContextProvider());
		CommonLocator.get().registerPropertyAccessor(GwittirBridge.get());
		CommonLocator.get().registerCurrentUtcDateProvider(
				new ClientUTCDateProvider());
		DataImageProvider.register(StandardDataImageProvider.get());
		TransformManager.get().setupClientListeners();
		TransformManager.get().addDomainTransformListener(
				PermissionsManager.get());
		ClientLayerLocator.get().setCommitToStorageTransformListener(
				createStorageTransformListener());
		ClientLayerLocator.get().registerTimerWrapperProvider(
				new TimerWrapperProviderGwt());
		registerExtraTransformListenersPreStorage();
		TransformManager.get().addDomainTransformListener(
				ClientLayerLocator.get().getCommitToStorageTransformListener());
		registerExtraTransformListenersPostStorage();
		CommonLocator.get().registerClassLookup(ClientReflector.get());
		CommonLocator.get().registerObjectLookup(TransformManager.get());
		CommonLocator.get().registerURLComponentEncoder(
				new ClientURLComponentEncoder());
	}

	protected void registerExtraTransformListenersPreStorage() {
	}

	protected void registerExtraTransformListenersPostStorage() {
	}

	protected ClientTransformManager createTransformManager() {
		return new ClientTransformManager();
	}

	protected void initExceptionHandling() {
		ClientLayerLocator.get().registerExceptionHandler(
				new ClientExceptionHandler());
	}

	protected void initNotifications() {
		ClientLayerLocator.get().registerNotifications(
				new ClientNotificationsImpl());
	}
}

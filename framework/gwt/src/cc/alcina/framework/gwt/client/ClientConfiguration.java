package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.LooseContext.ClientLooseContextProvider;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.ide.provider.DataImageProvider;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;
import cc.alcina.framework.gwt.client.logic.ClientUTCDateProvider;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.logic.state.MachineSchedulerGwt;
import cc.alcina.framework.gwt.client.provider.ClientURLComponentEncoder;
import cc.alcina.framework.gwt.client.res.AlcinaProperties;
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
		initCommonClient();
		initContentProvider();
		initImageProvider();
		prepareDebugFromHistory();
		extraConfiguration();
	}

	protected void prepareDebugFromHistory() {
		AlcinaHistory.initialiseDebugIds();
		if(AlcinaProperties.is(AlcinaProperties.class, AlcinaProperties.SIMULATE_OFFLINE)){
			AlcinaDebugIds.setFlag(AlcinaDebugIds.DEBUG_SIMULATE_OFFLINE);
		}
	}

	protected void initCss() {
		StyleInjector.inject(AlcinaResources.INSTANCE.css().getText());
	}


	protected void afterConfiguration() {
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
		LooseContext.register(new ClientLooseContextProvider());
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
		CommonLocator.get().registerMachineScheduler(new MachineSchedulerGwt());
	}

	protected void registerExtraTransformListenersPreStorage() {
	}

	protected void registerExtraTransformListenersPostStorage() {
	}

	protected ClientTransformManager createTransformManager() {
		return new ClientTransformManagerCommon();
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

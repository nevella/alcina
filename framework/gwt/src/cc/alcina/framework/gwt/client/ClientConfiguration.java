package cc.alcina.framework.gwt.client;

import java.util.Collections;

import com.google.gwt.dom.client.StyleInjector;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.context.LooseContext.ClientLooseContextProvider;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.DomainHandlerClient;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializerC;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.res.AlcinaProperties;
import cc.alcina.framework.gwt.client.res.AlcinaResources;
import cc.alcina.framework.gwt.client.util.TimerGwt;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

public class ClientConfiguration {
	protected void afterConfiguration() {
	}

	protected CommitToStorageTransformListener
			createStorageTransformListener() {
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
		ClientTransformManager.cast().setupClientListeners();
		TransformManager.get()
				.addDomainTransformListener(PermissionsManager.get());
		Registry.register().singleton(CommitToStorageTransformListener.class,
				createStorageTransformListener());
		Registry.register().singleton(Timer.Provider.class,
				new TimerGwt.Provider());
		registerExtraTransformListenersPreStorage();
		TransformManager.get().addDomainTransformListener(
				Registry.impl(CommitToStorageTransformListener.class));
		registerExtraTransformListenersPostStorage();
		// FIXME - reflection - remove (alcinabeanserializer -> elemental)
		Registry.register().add(AlcinaBeanSerializerC.class.getName(),
				Collections.singletonList(AlcinaBeanSerializer.class.getName()),
				Registration.Implementation.INSTANCE,
				Registration.Priority._DEFAULT);
	}

	protected void initContentProvider() {
	}

	protected void initCss() {
		StyleInjector.inject(AlcinaResources.INSTANCE.css().getText());
		StyleInjector.inject(AlcinaResources.INSTANCE.css24().getText());
	}

	protected void initExceptionHandling() {
		Registry.register().singleton(ClientExceptionHandler.class,
				new ClientExceptionHandler());
	}

	protected void initImageProvider() {
	}

	protected void initNotifications() {
		Registry.register().singleton(ClientNotifications.class,
				new ClientNotificationsImpl());
	}

	public void initServices() {
		initNotifications();
		initCss();
		Registry.impl(ClientNotifications.class).metricLogStart("moduleLoad");
		initExceptionHandling();
		initCommonClient();
		initContentProvider();
		initValidationFeedback();
		prepareDebugFromHistory();
		extraConfiguration();
	}

	protected void initValidationFeedback() {
		RelativePopupValidationFeedback.setupDefaultProvider();
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

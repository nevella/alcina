package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.state.Machine;
import cc.alcina.framework.common.client.state.MachineEvent.MachineEventImpl;
import cc.alcina.framework.common.client.state.MachineState;
import cc.alcina.framework.common.client.state.MachineState.MachineStateImpl;
import cc.alcina.framework.common.client.state.SimpleTransitionHandler;
import cc.alcina.framework.gwt.client.logic.state.AsyncCallbackTransitionHandler;

/*
 * Begin by just modelling CONFIG -> LOCAL PERSISTENCE -> AFTER_CONFIGURATION
 * then expand...
 */
public class ClientConfigurationMachine extends Machine<ClientConfigurationModel> {
	public static MachineStateImpl initialConfiguration = new MachineStateImpl(
			"initial-configuration");

	public static MachineStateImpl localPersistenceInit = new MachineStateImpl(
			"local-persistence-init");

	public static MachineStateImpl postLocalPersistenceInitConfig = new MachineStateImpl(
			"post-local-persistence-init-configuration");

	public static MachineEventImpl init = new MachineEventImpl("init", MachineState.START,
			initialConfiguration);

	public static MachineEventImpl initiallyConfigured = new MachineEventImpl(
			"initially-configured", initialConfiguration,
			localPersistenceInit);

	public static MachineEventImpl localPersistenceInitialised = new MachineEventImpl(
			"local-persistence-initialised", localPersistenceInit,
			postLocalPersistenceInitConfig);

	public static MachineEventImpl done = new MachineEventImpl("finish",
			postLocalPersistenceInitConfig, MachineState.END);
	

	public ClientConfigurationMachine() {
		model=new ClientConfigurationModel();
		model.setDebug(false);
		registerDefaultStatesEventsAndHandlers();
		registerTransitionHandler(MachineState.START, null,
				new SimpleTransitionHandler(init));
		registerTransitionHandler(initialConfiguration, null,
				new SimpleTransitionHandler(initiallyConfigured));
		registerTransitionHandler(localPersistenceInit, null,
				new SimpleTransitionHandler(localPersistenceInitialised));
	}


	public void replaceEvent(MachineStateImpl sourceState,
			MachineEventImpl successEvent) {
		AsyncCallbackTransitionHandler transitionHandler = 
				(AsyncCallbackTransitionHandler) getTransitionHandler(
				sourceState,
				null);
		transitionHandler.setSuccessEvent(successEvent);		
	}
}

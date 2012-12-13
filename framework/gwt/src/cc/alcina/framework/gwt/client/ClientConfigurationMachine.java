package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.state.Machine;
import cc.alcina.framework.common.client.state.MachineEvent.MachineEventImpl;
import cc.alcina.framework.common.client.state.MachineState;
import cc.alcina.framework.common.client.state.MachineState.MachineStateImpl;
import cc.alcina.framework.common.client.state.SimpleTransitionHandler;

/*
 * Begin by just modelling CONFIG -> LOCAL PERSISTENCE -> AFTER_CONFIGURATION
 * then expand...
 */
public class ClientConfigurationMachine extends Machine<ClientConfigurationModel> {
	MachineStateImpl initialConfiguration = new MachineStateImpl(
			"after-initial-configuration");

	public MachineStateImpl localPersistenceInit = new MachineStateImpl(
			"after-local-persistence-init");

	MachineStateImpl postLocalPersistenceInitConfig = new MachineStateImpl(
			"after-post-local-persistence-init-configuration");

	MachineEventImpl init = new MachineEventImpl("init", MachineState.START,
			initialConfiguration);

	public MachineEventImpl initiallyConfigured = new MachineEventImpl(
			"initially-configured", initialConfiguration,
			localPersistenceInit);

	public MachineEventImpl localPersistenceInitialised = new MachineEventImpl(
			"local-persistence-initialised", localPersistenceInit,
			postLocalPersistenceInitConfig);

	MachineEventImpl done = new MachineEventImpl("finish",
			postLocalPersistenceInitConfig, MachineState.END);
	

	public ClientConfigurationMachine() {
		model=new ClientConfigurationModel();
		model.setDebug(true);
		registerDefaultStatesEventsAndHandlers();
		registerTransitionHandler(MachineState.START, null,
				new SimpleTransitionHandler(init));
		registerTransitionHandler(initialConfiguration, null,
				new SimpleTransitionHandler(initiallyConfigured));
		registerTransitionHandler(localPersistenceInit, null,
				new SimpleTransitionHandler(localPersistenceInitialised));
	}
}

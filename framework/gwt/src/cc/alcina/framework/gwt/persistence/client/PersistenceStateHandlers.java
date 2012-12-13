package cc.alcina.framework.gwt.persistence.client;

import cc.alcina.framework.common.client.state.MachineEvent;
import cc.alcina.framework.gwt.client.ClientConfigurationModel;
import cc.alcina.framework.gwt.client.ClientLayerLocator;

public class PersistenceStateHandlers {

	public static class LocalPersistenceInitHandler
			extends
			PersistenceCallbackTransitionHandler<Void, ClientConfigurationModel> {
		public LocalPersistenceInitHandler(MachineEvent successEvent) {
			super(successEvent);
		}
	
		@Override
		public void onSuccess0(Void result) {
		}
	
		@Override
		public void start() {
			LocalTransformPersistence
			.registerLocalTransformPersistence(new WebDatabaseTransformPersistence());
			LocalTransformPersistence.get().init(new DTESerializationPolicy(),
					ClientLayerLocator.get().getCommitToStorageTransformListener(),
					this);
		}
	}
}

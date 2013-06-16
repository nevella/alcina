package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.state.Player.RunnableAsyncCallbackPlayer;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.persistence.client.DTESerializationPolicy;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;
import cc.alcina.framework.gwt.persistence.client.WebDatabaseTransformPersistence;

public class InitWebDbPlayer extends
		RunnableAsyncCallbackPlayer<Void, AsyncConfigConsortState> {
	private String transformDatabaseName;

	public InitWebDbPlayer() {
	}

	public InitWebDbPlayer(String transformDatabaseName) {
		this.transformDatabaseName = transformDatabaseName;
		addProvides(AsyncConfigConsortState.TRANSFORM_DB_INITIALISED);
	}

	@Override
	public void run() {
		LocalTransformPersistence
				.registerLocalTransformPersistence(new WebDatabaseTransformPersistence(
						transformDatabaseName));
		LocalTransformPersistence.get().init(new DTESerializationPolicy(),
				ClientLayerLocator.get().getCommitToStorageTransformListener(),
				this);
	}

	@Override
	public void onSuccess(Void result) {
		super.onSuccess(result);
	}
}
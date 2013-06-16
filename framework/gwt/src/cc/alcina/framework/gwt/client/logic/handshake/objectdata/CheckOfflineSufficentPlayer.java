package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;

/**
 * Subclass this if the app can function in a "never-connected-to-server" mode
 * 
 * @author nreddel@barnet.com.au
 * 
 */
public class CheckOfflineSufficentPlayer extends
		RunnablePlayer<LoadObjectDataState> {
	public CheckOfflineSufficentPlayer() {
		addRequires(LoadObjectDataState.LOADED_TRANSFORMS_FROM_LOCAL_STORAGE);
		addRequires(LoadObjectDataState.LOADED_CHUNKS_FROM_LOCAL_STORAGE);
		addProvides(LoadObjectDataState.OBJECT_DATA_LOADED);
		addProvides(LoadObjectDataState.OBJECT_DATA_LOAD_FAILED);
	}

	HandshakeConsortModel handshakeConsortModel = Registry
			.impl(HandshakeConsortModel.class);

	@Override
	public void run() {
		if (handshakeConsortModel.haveAllChunksNeededForOptimalObjectLoad()) {
			wasPlayed(LoadObjectDataState.OBJECT_DATA_LOADED);
		} else {
			wasPlayed(LoadObjectDataState.OBJECT_DATA_LOAD_FAILED);
		}
	}
}
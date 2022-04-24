package cc.alcina.framework.gwt.client.logic.handshake.objectdata;

import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.consort.Player.RunnablePlayer;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;

/**
 * Subclass this if the app can function in a "never-connected-to-server" mode
 *
 * @author nick@alcina.cc
 */
@Reflected
@Registration.Singleton
public class CheckOfflineSufficentPlayer
		extends RunnablePlayer<LoadObjectDataState> {
	HandshakeConsortModel handshakeConsortModel = Registry
			.impl(HandshakeConsortModel.class);

	public CheckOfflineSufficentPlayer() {
		addRequires(
				LoadObjectDataState.LOADED_DELTA_APPLICATIONS_FROM_LOCAL_STORAGE);
		addProvides(LoadObjectDataState.DELTA_STORE_MERGED_IF_NECESSARY);
		addProvides(LoadObjectDataState.OBJECT_DATA_LOADED);
		addProvides(LoadObjectDataState.OBJECT_DATA_LOAD_FAILED);
	}

	@Override
	public void run() {
		if (handshakeConsortModel.haveAllChunksNeededForOptimalObjectLoad()) {
			handshakeConsortModel.ensureClientInstanceFromModelDeltas();
			List<LoadObjectDataState> states = Arrays.asList(
					LoadObjectDataState.DELTA_STORE_MERGED_IF_NECESSARY,
					LoadObjectDataState.OBJECT_DATA_LOADED);
			consort.wasPlayed(this, states);
		} else {
			wasPlayed(LoadObjectDataState.OBJECT_DATA_LOAD_FAILED);
		}
	}
}

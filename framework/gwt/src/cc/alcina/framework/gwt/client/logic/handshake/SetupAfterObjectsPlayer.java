package cc.alcina.framework.gwt.client.logic.handshake;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ide.provider.ContentProvider;
import cc.alcina.framework.gwt.client.logic.DevCSSHelper;

@ClientInstantiable
public abstract class SetupAfterObjectsPlayer extends RunnablePlayer {
	public SetupAfterObjectsPlayer() {
		addRequires(HandshakeState.OBJECTS_UNWRAPPED_AND_REGISTERED);
		addProvides(HandshakeState.SETUP_AFTER_OBJECTS_LOADED);
	}
	
}
package cc.alcina.template.client.handshake;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.gwt.client.logic.handshake.SetupAfterObjectsPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.localstorage.StandardWebDbSetupAfterObjectsPlayer;
@RegistryLocation(registryPoint = SetupAfterObjectsPlayer.class, implementationType = ImplementationType.SINGLETON)
public  class AlcinaTemplateSetupAfterObjectsPlayer extends
		StandardWebDbSetupAfterObjectsPlayer {
	public AlcinaTemplateSetupAfterObjectsPlayer() {
		super();
	}
}
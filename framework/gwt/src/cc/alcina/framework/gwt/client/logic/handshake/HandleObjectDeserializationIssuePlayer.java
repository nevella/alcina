package cc.alcina.framework.gwt.client.logic.handshake;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.state.EndpointPlayer;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd.ReloadOnSuccessCallback;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;

@ClientInstantiable
@RegistryLocation(registryPoint = HandleObjectDeserializationIssuePlayer.class, implementationType = ImplementationType.SINGLETON)
public class HandleObjectDeserializationIssuePlayer  extends EndpointPlayer<HandshakeState> {
	public HandleObjectDeserializationIssuePlayer() {
		super(HandshakeState.OBJECTS_FATAL_DESERIALIZATION_EXCEPTION);
	}

	@Override
	public void run() {
		if (LocalTransformPersistence.get() != null) {
			Window.alert("Failure in unwrap/register -  press 'OK' to clear and reload");
			LocalTransformPersistence.get().clearPersistedClient(null, -1,
					new ReloadOnSuccessCallback(), true);
		} else {
			Window.alert("Failure in unwrap/register -  press 'OK' to reload");
			new ReloadOnSuccessCallback().onSuccess(null);
		}
	}
}
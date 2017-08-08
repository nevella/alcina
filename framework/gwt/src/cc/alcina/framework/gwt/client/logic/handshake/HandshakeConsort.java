package cc.alcina.framework.gwt.client.logic.handshake;

import java.util.Collections;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.ConsortWithSignals;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

/**
 * <h3>Per-session handshake persistence</h3>
 * <p>
 * Alcina supports resuming the most recent session (tab) when restarting the
 * application when offline - most of the complexity of the handshake process is
 * a result of managing that.
 * </p>
 * <p>
 * Client initialisation can either overwrite the existing object chunk - or
 * modify it with a delta (sometimes with radically reduced download
 * size/startup time) - in any case, at the end of an online handshake, the
 * tab's clientinstance will be the owner of the object chunks.
 * </p>
 * <p>
 * If online, those chunks will be the only source of the client's initial
 * object graph. If offline, any transforms generated (by app sessions using the
 * same client instance) will also be replayed.
 * </p>
 * <h3>Notes:</h3>
 * <ul>
 * <li>If you use local persistence, make sure to call <br>
 * <code>new SaveToLocalStorageConsort().start();</code><br>
 * at some point after object unwrap/register
 * </ul>
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = HandshakeConsort.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class HandshakeConsort extends
		ConsortWithSignals<HandshakeState, HandshakeSignal> {
	public static final String TOPIC_STARTUP_PROGRESS = HandshakeConsort.class.getName()
	+ ".TOPIC_STARTUP_PROGRESS";

	public HandshakeConsort() {
	}

	public boolean isAfterDomainModelLoaded() {
		return containsState(HandshakeState.SETUP_AFTER_OBJECTS_LOADED);
	}

	public void restartFromServices() {
		HandshakeConsortModel.get().clearObjects();
		Registry.impl(HandshakeConsortModel.class).setLoginResponse(null);
		removeStates(Collections.singleton(UploadOfflineTransformsPlayer.OFFLINE_TRANSFORMS_UPLOADED));
		signal(HandshakeSignal.OBJECTS_INVALIDATED);
	}

	public void logout() {
		HandshakeConsortModel.get().clearObjects();
		Registry.impl(HandshakeConsortModel.class).setLoginResponse(null);
		signal(HandshakeSignal.LOGGED_OUT);
	}

	public void handleLoggedIn(LoginResponse loginResponse) {
		handleLoggedIn(loginResponse, null);
	}

	public void handleLoggedIn(LoginResponse loginResponse,
			AsyncCallback handshakeFinishedCallback) {
		HandshakeConsortModel.get().clearObjects();
		Registry.impl(HandshakeConsortModel.class).setLoginResponse(
				loginResponse);
		signal(HandshakeSignal.LOGGED_IN, handshakeFinishedCallback);
	}

	public static void startupProgress(String message) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_STARTUP_PROGRESS, message);
	}

	public static void startupProgressListenerDelta(TopicListener<String> listener, boolean add) {
		GlobalTopicPublisher.get().listenerDelta(TOPIC_STARTUP_PROGRESS, listener, add);
	}
}
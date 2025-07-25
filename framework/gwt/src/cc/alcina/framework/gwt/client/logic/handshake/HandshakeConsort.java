package cc.alcina.framework.gwt.client.logic.handshake;

import java.util.Collections;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.consort.ConsortWithSignals;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentSingleton;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Topic;

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
 * 
 */
@Reflected
@Registration.Singleton
@EnvironmentSingleton
public class HandshakeConsort
		extends ConsortWithSignals<HandshakeState, HandshakeSignal> {
	public static final Topic<String> topicStartupProgress = Topic.create();

	public static HandshakeConsort get() {
		return Registry.impl(HandshakeConsort.class);
	}

	public void handleLoggedIn(LoginResponse loginResponse) {
		handleLoggedIn(loginResponse, null);
	}

	public void handleLoggedIn(LoginResponse loginResponse,
			AsyncCallback handshakeFinishedCallback) {
		HandshakeConsortModel.get().clearObjects();
		Registry.impl(HandshakeConsortModel.class)
				.setLoginResponse(loginResponse);
		signal(HandshakeSignal.LOGGED_IN, handshakeFinishedCallback);
	}

	public boolean isAfterDomainModelLoaded() {
		return containsState(HandshakeState.SETUP_AFTER_OBJECTS_LOADED);
	}

	public void logout() {
		HandshakeConsortModel.get().clearObjects();
		Registry.impl(HandshakeConsortModel.class).setLoginResponse(null);
		signal(HandshakeSignal.LOGGED_OUT);
	}

	public void restartFromServices() {
		HandshakeConsortModel.get().clearObjects();
		Registry.impl(HandshakeConsortModel.class).setLoginResponse(null);
		removeStates(Collections.singleton(
				UploadOfflineTransformsPlayer.OFFLINE_TRANSFORMS_UPLOADED));
		signal(HandshakeSignal.OBJECTS_INVALIDATED);
	}
}

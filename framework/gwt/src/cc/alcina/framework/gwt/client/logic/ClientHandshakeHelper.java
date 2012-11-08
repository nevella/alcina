package cc.alcina.framework.gwt.client.logic;

import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.common.client.logic.StateListenable;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelHolder;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ClientMetricLogging;
import cc.alcina.framework.gwt.client.ide.provider.ContentProvider;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public abstract class ClientHandshakeHelper extends StateListenable implements
		StateChangeListener {
	public static final String STATE_PRE_HELLO = "STATE_PRE_HELLO";

	public static final String STATE_AFTER_HELLO = "STATE_AFTER_HELLO";

	public static final String STATE_BEFORE_MODEL_LOAD = "STATE_BEFORE_MODEL_LOAD";

	public static final String STATE_DOMAIN_MODEL_REGISTERED = "STATE_DOMAIN_MODEL_REGISTERED";

	public static final String STATE_AFTER_DOMAIN_MODEL_REGISTERED = "STATE_AFTER_DOMAIN_MODEL_REGISTERED";

	private LoginResponse loginResponse;

	public ClientHandshakeHelper() {
		addStateChangeListener(this);
	}

	public void beginHandshake() {
		fireStateChanged(STATE_PRE_HELLO);
	}

	public LoginResponse getLoginResponse() {
		return loginResponse;
	}

	public void setLoginResponse(LoginResponse loginResponse) {
		this.loginResponse = loginResponse;
	}

	public boolean permitsOfflineWithEmptyTransforms() {
		return false;
	}

	public void stateChanged(Object source, String newState) {
		if (newState.equals(STATE_PRE_HELLO)) {
			boolean logLoadMetrics = AlcinaDebugIds
					.hasFlag(AlcinaDebugIds.DEBUG_LOG_LOAD_METRICS);
			if (!logLoadMetrics) {
				ClientMetricLogging.get().setMuted(true);
			}
			updateUIPreHello();
			doHello();
		}
		if (newState.equals(STATE_DOMAIN_MODEL_REGISTERED)) {
			ClientMetricLogging.get().setMuted(false);
			afterDomainModelRegistration();
			fireStateChanged(STATE_AFTER_DOMAIN_MODEL_REGISTERED);
			ClientLayerLocator.get().getCommitToStorageTransformListener().setPaused(false);
		}
	}

	protected void doHello() {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public void execute() {
				hello();
			}
		});
	}

	protected void updateUIPreHello() {
	}

	public void clearPerInstanceState() {
		TransformManager.get().clearUserObjects();
		if (ClientLayerLocator.get().getDomainModelHolder() != null) {
			DevCSSHelper.get().removeCssListeners(
					ClientLayerLocator.get().getGeneralProperties());
			registerClientObjectListeners(false);
		}
	}

	protected void registerClientObjectListeners(boolean register) {
	}

	public void registerDomainModel(DomainModelHolder objects,
			LoginState loginState) {
		registerDomainModel(objects, loginState, null);
	}

	public void registerDomainModel(final DomainModelHolder objects,
			final LoginState loginState, final ScheduledCommand postRegisterCommand) {
		objects.registerSelfAsProvider();
		registerClientObjectListeners(true);
		ClientLayerLocator.get().setGeneralProperties(
				objects.getGeneralProperties());
		PermissionsManager.get().setUser(objects.getCurrentUser());
		ClientLayerLocator.get().setDomainModelHolder(objects);
		PermissionsManager.get().setLoginState(loginState);
		if (!TransformManager.get().isReplayingRemoteEvent()) {
			final ScheduledCommand postRegisterCleanupCommand=new ScheduledCommand() {

				@Override
				public void execute() {
					ClientMetricLogging.get().end("register-domain");
					if (PermissionsManager.get().getOnlineState() != OnlineState.OFFLINE) {
						locallyPersistDomainModelAndReplayPostLoadTransforms(
								loginState, postRegisterCommand);
						// note, this'll do afterLocal via a callback, hence the return
					}else{
						afterLocalPersistenceAndReplay(loginState, postRegisterCommand);	
					}					
				}
				
			};
			ScheduledCommand registerCommand=new ScheduledCommand() {

				@Override
				public void execute() {
					ClientMetricLogging.get().start("register-domain");
					TransformManager.get().registerDomainObjectsInHolderAsync(objects,postRegisterCleanupCommand);
					
				}
			};
			if(postRegisterCommand!=null){
				Scheduler.get().scheduleDeferred(registerCommand);
			}else{
				ClientMetricLogging.get().start("register-domain");
				TransformManager.get().registerDomainObjectsInHolder(objects);
				postRegisterCleanupCommand.execute();
			}
			return;
		}
		afterLocalPersistenceAndReplay(loginState, postRegisterCommand);
	}

	public void afterLocalPersistenceAndReplay(LoginState loginState,
			ScheduledCommand postRegisterCommand) {
		registerObjectsPostDomainLoad();
		if (postRegisterCommand != null) {
			postRegisterCommand.execute();
		}
	}

	protected void registerObjectsPostDomainLoad() {
		DevCSSHelper.get().addCssListeners(
				ClientLayerLocator.get().getGeneralProperties());
		ContentProvider.refresh();
	}

	/**
	 * see <tt>ClientHandshakeHelperWithLocalPersistence</tt>
	 * 
	 * @param postRegisterCommand
	 * 
	 * @param loadHelper
	 */
	protected void locallyPersistDomainModelAndReplayPostLoadTransforms(
			LoginState loginState, ScheduledCommand postRegisterCommand) {
		afterLocalPersistenceAndReplay(loginState, postRegisterCommand);
	}

	protected abstract void afterDomainModelRegistration();

	protected abstract void hello();

	public abstract void handleLoggedIn(LoginResponse lrb);

	public abstract void logout();
}

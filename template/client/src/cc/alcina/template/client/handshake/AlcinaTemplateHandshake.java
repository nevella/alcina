package cc.alcina.template.client.handshake;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.Player;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.logic.handshake.AllowObjectsLoadFailedPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.HandleLoggedInSignalHandler;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsort;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;
import cc.alcina.framework.gwt.client.logic.handshake.InitLayoutPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.InitLoaderUiPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.InitServicesAsyncAndSyncPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.InitSynchronousServicesPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.LogoutWithReloadSignalHandler;
import cc.alcina.framework.gwt.client.logic.handshake.SetupAfterObjectsPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.StartAppPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.UnwrapAndRegisterObjectsPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.UploadOfflineTransformsPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.WaitForAppCachePlayer;
import cc.alcina.framework.gwt.client.logic.handshake.localstorage.RetrieveLocalModelTransformDeltasPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.objectdata.LoadObjectsFromRemotePlayer;
import cc.alcina.framework.gwt.client.logic.handshake.objectdata.LoadObjectsPlayer;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.template.client.AlcinaTemplateConfiguration;

@ClientInstantiable
public class AlcinaTemplateHandshake {
	public AlcinaTemplateHandshake() {
	}

	protected HandshakeConsort consort;

	private TopicListener notificationUpdateListener = new TopicListener<Player>() {
		@Override
		public void topicPublished(String key, Player player) {
			if (player instanceof LoadObjectsFromRemotePlayer
					|| player instanceof RetrieveLocalModelTransformDeltasPlayer) {
				ModalNotifier modalNotifier = Registry.impl(
						HandshakeConsortModel.class).ensureLoadObjectsNotifier(
						"Loading data");
				modalNotifier.modalOn();
				modalNotifier.setMasking(false);
			}
			if (player instanceof LoadObjectsPlayer) {
				ModalNotifier modalNotifier = Registry.impl(
						HandshakeConsortModel.class).ensureLoadObjectsNotifier(
						"Checking local data");
				modalNotifier.modalOn();
				modalNotifier.setMasking(false);
			}
		}
	};

	public void run() {
		setupConsort();
		consort.start();
	}

	protected void setupConsort() {
		consort = Registry.impl(HandshakeConsort.class);
		consort.addPlayer(new InitSynchronousServicesPlayer(
				getClientConfiguration()));
		consort.addPlayer(new AlcinaTemplateInitAysncServicesPlayer());
		consort.addPlayer(Registry.impl(InitLoaderUiPlayer.class));
		consort.addPlayer(new InitServicesAsyncAndSyncPlayer());
		consort.addPlayer(new WaitForAppCachePlayer());
		consort.addPlayer(new UploadOfflineTransformsPlayer()).addRequires(
				WaitForAppCachePlayer.APP_CACHE_CHECKED);
		consort.addPlayer(new LoadObjectsPlayer()).addRequires(
				UploadOfflineTransformsPlayer.OFFLINE_TRANSFORMS_UPLOADED);
		consort.addPlayer(new UnwrapAndRegisterObjectsPlayer());
		consort.addPlayer(new AllowObjectsLoadFailedPlayer());
		consort.addPlayer(Registry.impl(SetupAfterObjectsPlayer.class));
		consort.addPlayer(new InitLayoutPlayer());
		consort.addPlayer(new StartAppPlayer());
		consort.addSignalHandler(new LogoutWithReloadSignalHandler());
		consort.addSignalHandler(new HandleLoggedInSignalHandler());
		consort.listenerDelta(Consort.BEFORE_PLAY, notificationUpdateListener,
				true);
	}

	protected AlcinaTemplateConfiguration getClientConfiguration() {
		return Registry.impl(AlcinaTemplateConfiguration.class);
	}
}

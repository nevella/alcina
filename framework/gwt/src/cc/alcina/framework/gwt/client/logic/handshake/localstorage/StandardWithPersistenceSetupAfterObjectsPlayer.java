package cc.alcina.framework.gwt.client.logic.handshake.localstorage;

import java.util.Arrays;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.logic.DevCSSHelper;
import cc.alcina.framework.gwt.client.logic.handshake.SetupAfterObjectsPlayer;
import cc.alcina.framework.gwt.client.util.AsyncCallbackStd;
import cc.alcina.framework.gwt.persistence.client.DatabaseStatsObserver;

public abstract class StandardWithPersistenceSetupAfterObjectsPlayer extends
		SetupAfterObjectsPlayer {
	private SaveToLocalStorageConsort saveConsort;

	DatabaseStatsObserver statsObserver = Registry
			.impl(DatabaseStatsObserver.class);

	protected AsyncCallback reportListener = new AsyncCallbackStd() {
		@Override
		public void onSuccess(Object result) {
			AlcinaTopics
					.logCategorisedMessage(new StringPair(
							AlcinaTopics.LOG_CATEGORY_MESSAGE,
							"After object serialization:\n"
									+ statsObserver.getReport()));
			statsObserver.installPersistenceListeners();
		}
	};

	private TopicListener finishedListener = new TopicListener() {
		@Override
		public void topicPublished(String key, Object message) {
			statsObserver.recalcWithListener(reportListener);
			saveConsort.deferredRemove(Arrays.asList(Consort.FINISHED),
					finishedListener);
		}
	};

	public StandardWithPersistenceSetupAfterObjectsPlayer() {
		super();
	}

	@Override
	public void run() {
		DevCSSHelper.get().addCssListeners(ClientBase.getGeneralProperties());
		saveToLocalPersistenceAndStat();
	}

	protected void saveToLocalPersistenceAndStat() {
		if (PermissionsManager.isOnline()) {
			saveConsort = new SaveToLocalStorageConsort();
			saveConsort.start();
			saveConsort.listenerDelta(Consort.FINISHED, finishedListener, true);
		}
	}
}
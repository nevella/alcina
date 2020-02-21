package cc.alcina.template.client.handshake;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.ConsortPlayer;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.logic.handshake.AsyncConfigConsort;
import cc.alcina.framework.gwt.client.logic.handshake.AsyncConfigConsortState;
import cc.alcina.framework.gwt.client.logic.handshake.InitAysncServicesPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.InitPropAndLogDbPlayer;
import cc.alcina.framework.gwt.client.logic.handshake.InitWebDbPlayer;
import cc.alcina.framework.gwt.client.widget.complex.StatusPanel;
import cc.alcina.framework.gwt.persistence.client.DatabaseStatsCollector;
import cc.alcina.framework.gwt.persistence.client.DatabaseStatsInfo;
import cc.alcina.framework.gwt.persistence.client.DatabaseStatsObserver;
import cc.alcina.framework.gwt.persistence.client.RemoteLogPersister;
import cc.alcina.template.client.AlcinaTemplateConfiguration;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AlcinaTemplateInitAysncServicesPlayer extends InitAysncServicesPlayer
		implements ConsortPlayer {
	static class CheckDbStatsPlayer extends
			RunnableAsyncCallbackPlayer<Void, AsyncConfigConsortState> {
		CheckDbStatsPlayer() {
			addRequires(AsyncConfigConsortState.LOG_DB_INITIALISED);
			addProvides(PRE_HANDSHAKE_DB_STATS_CHECKED);
		}

		@Override
		public void run() {
			if (History.getToken().equals("database-stats")) {
				AsyncCallback<DatabaseStatsInfo> logDbStatsCallback = new AsyncCallback<DatabaseStatsInfo>() {
					@Override
					public void onFailure(Throwable caught) {
						throw new WrappedRuntimeException(caught);
					}

					@Override
					public void onSuccess(DatabaseStatsInfo value) {
						Registry.impl(ClientNotifications.class)
								.log(value.toString());
						StatusPanel
								.showMessageOrAlert(Ax.format(
												"<pre style='display:inline-block;text-align:left'>%s</pre>",
												value.toString()));
					}
				};
				new DatabaseStatsCollector().run(logDbStatsCallback);
				return;// finish - do not continue the consort
			} else {
				wasPlayed();
			}
		}
	}

	static class ObserveDbStatsPlayer extends
			RunnableAsyncCallbackPlayer<String, AsyncConfigConsortState> {
		ObserveDbStatsPlayer() {
			addRequires(PRE_HANDSHAKE_DB_STATS_CHECKED);
			addProvides(INSTALLED_DB_STATS_OBSERVER);
		}

		@Override
		public void run() {
			Registry.impl(DatabaseStatsObserver.class).init(this);
		}

		@Override
		public void onSuccess(String result) {
			AlcinaTopics.logCategorisedMessage(new StringPair(
					AlcinaTopics.LOG_CATEGORY_MESSAGE, "Before object load:\n"+Registry.impl(
							DatabaseStatsObserver.class).getReport()));
			wasPlayed();
		}
	}

	private AsyncConfigConsort asyncConfigConsort;

	public static final AsyncConfigConsortState PRE_HANDSHAKE_DB_STATS_CHECKED = new AsyncConfigConsortState(
			"PRE_HANDSHAKE_DB_STATS_CHECKED");

	public static final AsyncConfigConsortState INSTALLED_DB_STATS_OBSERVER = new AsyncConfigConsortState(
			"INSTALLED_DB_STATS_OBSERVER");

	public AlcinaTemplateInitAysncServicesPlayer() {
		this.asyncConfigConsort = new AsyncConfigConsort();
		asyncConfigConsort.addPlayer(new InitWebDbPlayer(Registry.impl(
				AlcinaTemplateConfiguration.class).getTransformDbName()));
		RemoteLogPersister remoteLogPersister = new RemoteLogPersister();
		asyncConfigConsort.addPlayer(new InitPropAndLogDbPlayer(Registry.impl(
				AlcinaTemplateConfiguration.class).getTransformDbName(),
				remoteLogPersister));
		asyncConfigConsort.addPlayer(new CheckDbStatsPlayer());
		asyncConfigConsort.addPlayer(new ObserveDbStatsPlayer());
		asyncConfigConsort.addEndpointPlayer();
	}

	@Override
	public void run() {
		new SubconsortSupport().run(consort, asyncConfigConsort, this);
	}

	@Override
	public Consort getStateConsort() {
		return asyncConfigConsort;
	}
}
package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.state.MachineEvent;
import cc.alcina.framework.common.client.state.MachineEvent.MachineEventImpl;
import cc.alcina.framework.common.client.state.MachineModel;
import cc.alcina.framework.common.client.state.MachineState.MachineStateImpl;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.ClientConfigurationMachine;
import cc.alcina.framework.gwt.client.ClientConfigurationModel;
import cc.alcina.framework.gwt.client.ClientLayerLocator;

import com.google.code.gwt.database.client.Database;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.History;

public class PersistenceStateHandlers {
	public static class LocalPersistenceInitHandler
			extends
			PersistenceCallbackTransitionHandler<Void, ClientConfigurationModel> {
		public LocalPersistenceInitHandler(MachineEvent successEvent) {
			super(successEvent);
		}

		@Override
		public void onSuccess0(Void result) {
		}

		@Override
		public void start() {
			LocalTransformPersistence
					.registerLocalTransformPersistence(new WebDatabaseTransformPersistence());
			LocalTransformPersistence.get().init(
					new DTESerializationPolicy(),
					ClientLayerLocator.get()
							.getCommitToStorageTransformListener(), this);
		}
	}

	public static MachineStateImpl localPersistenceInitPropertyStore = new MachineStateImpl(
			"local-persistence-init-property-store");

	public static MachineStateImpl localPersistenceInitLogStore = new MachineStateImpl(
			"local-persistence-init-log-store");

	public static MachineEventImpl localPersistenceInitialised = new MachineEventImpl(
			"local-persistence-initialised",
			ClientConfigurationMachine.localPersistenceInit,
			localPersistenceInitPropertyStore);

	public static MachineEventImpl propertyStoreInitialised = new MachineEventImpl(
			"local-persistence-initialised-property-store",
			localPersistenceInitPropertyStore, localPersistenceInitLogStore);

	public static MachineEventImpl logStoreInitialised = new MachineEventImpl(
			"local-persistence-initialised-log-store",
			localPersistenceInitLogStore,
			ClientConfigurationMachine.postLocalPersistenceInitConfig);

	public static class PersistencePropAndLogWebDbInitaliser {
		class InitCallback extends PersistenceCallbackTransitionHandler {
			public InitCallback(MachineEvent successEvent) {
				super(successEvent);
			}

			@Override
			public void onSuccess(Object result) {
				iterate();
			}

			@Override
			public void performTransition(MachineModel model) {
				this.model = model;
				iterate();
			}

			@Override
			public void onSuccess0(Object result) {
			}

			@Override
			public void start() {
			}

			@Override
			public void afterSuccess() {
				super.afterSuccess();
			};
		}

		private enum State {
			PRE_PROPERTY_IMPL, POST_PROPERTY_IMPL, PRE_LOG_IMPL, POST_LOG_IMPL
		}

		private State state = State.PRE_PROPERTY_IMPL;

		private final String dbPrefix;

		private InitCallback itrCallback;

		private ObjectStoreWebDbImpl propImpl;

		private ObjectStoreWebDbImpl logImpl;

		private ClientConfigurationMachine machine;

		private final RemoteLogPersister remoteLogPersister;

		public PersistencePropAndLogWebDbInitaliser(String dbPrefix,
				ClientConfigurationMachine machine,
				RemoteLogPersister remoteLogPersister,
				MachineEventImpl successEvent) {
			this.dbPrefix = dbPrefix;
			this.machine = machine;
			this.remoteLogPersister = remoteLogPersister;
			itrCallback = new InitCallback(successEvent);
			registerStatesAndEvents();
		}

		private void registerStatesAndEvents() {
			machine.replaceEvent(
					ClientConfigurationMachine.localPersistenceInit,
					localPersistenceInitialised);
			machine.registerTransitionHandler(
					localPersistenceInitPropertyStore, null, itrCallback);
			machine.registerTransitionHandler(localPersistenceInitLogStore,
					null, itrCallback);
		}

		public void start() {
			iterate();
		}

		private void iterate() {
			switch (state) {
			case PRE_PROPERTY_IMPL: {
				Database db = Database.openDatabase(dbPrefix, "1.0",
						"Property store", 5000000);
				this.propImpl = new ObjectStoreWebDbImpl(db, dbPrefix
						+ "_propertyStore", itrCallback);
				state = State.POST_PROPERTY_IMPL;
				break;
			}
			case POST_PROPERTY_IMPL:
				PropertyStore.get().registerDelegate(propImpl);
				state = State.PRE_LOG_IMPL;
				iterate();
				break;
			case PRE_LOG_IMPL:
				Database db = Database.openDatabase(dbPrefix, "1.0",
						"Log store", 5000000);
				this.logImpl = new ObjectStoreWebDbImpl(db, dbPrefix
						+ "_logStore", itrCallback);
				state = State.POST_LOG_IMPL;
				break;
			case POST_LOG_IMPL:
				LogStore.get().registerDelegate(logImpl);
				if (remoteLogPersister != null) {
					LogStore.get().setRemoteLogPersister(remoteLogPersister);
					remoteLogPersister.push();
				}
				itrCallback.afterSuccess();
				break;
			}
		}
	}

	public static class LogStoreInterceptors {
		private ValueChangeHandler<String> historyListener = new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				AlcinaTopics.logCategorisedMessage(new StringPair(
						AlcinaTopics.LOG_CATEGORY_HISTORY, event.getValue()));
			}
		};

		private int statsMuteCounter = 0;

		private TopicListener<Boolean> muteListener = new TopicListener<Boolean>() {
			@Override
			public void topicPublished(String key, Boolean message) {
				statsMuteCounter += message ? 1 : -1;
			}
		};

		public void installStats() {
			AlcinaTopics.muteStatisticsLoggingListenerDelta(muteListener, true);
			installStats0();
		}

		native void installStats0()/*-{
			function format(out) {
				var idx = 0;
				var j = 1;

				while (true) {
					idx = out.indexOf("%s", idx);
					if (idx == -1) {
						break;
					}
					var ins = arguments[j++];
					if (ins === null) {
						ins = "null";
					} else if (ins === undefined) {
						ins = "undefined";
					} else {
						ins = ins.toString();
					}
					out = out.substring(0, idx) + ins + out.substring(idx + 2);
					idx += ins.length;
				}
				return out;
			}
			function pad0(s, len) {
				return pad(s, "0", len);
			}
			function pad(s, sup, len) {
				s = "" + s;
				while (s.length < len) {
					s = sup + s;
				}
				return s;
			}
			var lsi = this;
			var running = [];
			function eventToString(event) {
				// return some string representation of this event
				var d = new Date(event.millis);
				var timeStr = format("%s:%s:%s,%s", pad0(d.getHours(), 2), pad0(d
						.getMinutes(), 2), pad0(d.getSeconds(), 2), pad0(d
						.getMilliseconds(), 3));
				return event.evtGroup + " | " + event.moduleName + " | "
						+ event.subSystem + " | " + event.method + " | "
						+ pad(event.type, " ", 25) + +" | " + timeStr;
			}
			window.$stats = function(evt) {
				var muted = lsi.@cc.alcina.framework.gwt.persistence.client.PersistenceStateHandlers.LogStoreInterceptors::areStatsMuted()();
				if (!muted) {
					var e2s = eventToString(evt);
					lsi.@cc.alcina.framework.gwt.persistence.client.PersistenceStateHandlers.LogStoreInterceptors::logMetric(Ljava/lang/String;)(e2s);
				}
				return true;
			};

		}-*/;

		boolean areStatsMuted() {
			return statsMuteCounter > 0;
		}

		private HandlerRegistration historyHandlerRegistration;

		private HandlerRegistration nativePreviewHandlerRegistration;

		public void interceptClientLog() {
			AlcinaTopics.logCategorisedMessageListenerDelta(LogStore.get()
					.getStringPairListener(), true);
		}

		public void logMetric(String stat) {
			ClientLayerLocator.get().notifications().log(stat);
		}

		public void logHistoryEvents() {
			this.historyHandlerRegistration = History
					.addValueChangeHandler(historyListener);
		}

		public void logClicks() {
			nativePreviewHandlerRegistration = Event
					.addNativePreviewHandler(new NativePreviewHandler() {
						public void onPreviewNativeEvent(
								NativePreviewEvent event) {
							previewNativeEvent(event);
						}
					});
		}

		protected void previewNativeEvent(NativePreviewEvent event) {
			Event nativeEvent = Event.as(event.getNativeEvent());
			if (BrowserEvents.CLICK.equals(nativeEvent.getType())) {
				EventTarget eTarget = nativeEvent.getEventTarget();
				if (Element.is(eTarget)) {
					List<String> tags = new ArrayList<String>();
					Element e = Element.as(eTarget);
					while (e != null) {
						List<String> parts = new ArrayList<String>();
						parts.add(e.getTagName());
						if (!e.getId().isEmpty()) {
							parts.add("#" + e.getId());
						}
						if (!e.getClassName().isEmpty()) {
							parts.add("." + e.getClassName());
						}
						tags.add(CommonUtils.join(parts, ""));
						e = e.getParentElement();
					}
					Collections.reverse(tags);
					String path = CommonUtils.join(tags, "/");
					AlcinaTopics.logCategorisedMessage(new StringPair(
							AlcinaTopics.LOG_CATEGORY_CLICK, path));
				}
			}
		}

		public void unload() {
			AlcinaTopics.logCategorisedMessageListenerDelta(LogStore.get()
					.getStringPairListener(), false);
			AlcinaTopics
					.muteStatisticsLoggingListenerDelta(muteListener, false);
			if (historyHandlerRegistration != null) {
				historyHandlerRegistration.removeHandler();
			}
			if (nativePreviewHandlerRegistration != null) {
				nativePreviewHandlerRegistration.removeHandler();
			}
		}
	}
}

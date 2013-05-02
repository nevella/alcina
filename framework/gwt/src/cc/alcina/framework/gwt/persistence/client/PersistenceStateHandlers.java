package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.state.MachineEvent;
import cc.alcina.framework.common.client.state.MachineEvent.MachineEventImpl;
import cc.alcina.framework.common.client.state.MachineModel;
import cc.alcina.framework.common.client.state.MachineState.MachineStateImpl;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.ClientConfigurationMachine;
import cc.alcina.framework.gwt.client.ClientConfigurationModel;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.util.ClientNodeIterator;
import cc.alcina.framework.gwt.client.util.TextUtils;

import com.google.code.gwt.database.client.Database;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Text;
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
						+ pad(event.type, " ", 25) + " | " + timeStr;
			}
			window.$stats = function(evt) {
				var muted = lsi.@cc.alcina.framework.gwt.persistence.client.PersistenceStateHandlers.LogStoreInterceptors::areStatsMuted()();
				if (!muted) {
					var e2s = eventToString(evt);
					lsi.@cc.alcina.framework.gwt.persistence.client.PersistenceStateHandlers.LogStoreInterceptors::logStat(Ljava/lang/String;)(e2s);
				}
				return true;
			};
			//if there were stats collected prior to this install, flush 'em
			if (window["stats_pre"]) {
				for ( var k in window.stats_pre) {
					var pre = window.stats_pre[k];
					lsi.@cc.alcina.framework.gwt.persistence.client.PersistenceStateHandlers.LogStoreInterceptors::logStat(Ljava/lang/String;)(pre);
				}
				window.$stats_pre = [];
			}
			if ($wnd["stats_pre"]) {
				for ( var k in $wnd.stats_pre) {
					var pre = $wnd.stats_pre[k];
					lsi.@cc.alcina.framework.gwt.persistence.client.PersistenceStateHandlers.LogStoreInterceptors::logStat(Ljava/lang/String;)(pre);
				}
				$wnd.$stats_pre = [];
			}

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

		public void logStat(String stat) {
			AlcinaTopics.logCategorisedMessage(new StringPair(
					AlcinaTopics.LOG_CATEGORY_STAT, stat));
		}

		public void logHistoryEvents() {
			this.historyHandlerRegistration = History
					.addValueChangeHandler(historyListener);
		}

		public void logClicksAndChanges() {
			nativePreviewHandlerRegistration = Event
					.addNativePreviewHandler(new NativePreviewHandler() {
						public void onPreviewNativeEvent(
								NativePreviewEvent event) {
							previewNativeEvent(event);
						}
					});
		}
		
		private String lastFocussedValueMessage;
		protected void previewNativeEvent(NativePreviewEvent event) {
			Event nativeEvent = Event.as(event.getNativeEvent());
			String type = nativeEvent.getType();
			boolean click = BrowserEvents.CLICK.equals(type);
			boolean blur = BrowserEvents.BLUR.equals(type)||BrowserEvents.FOCUSOUT.equals(type);
			boolean focus = BrowserEvents.FOCUS.equals(type)||BrowserEvents.FOCUSIN.equals(type);
			if (click||blur||focus) {
 				EventTarget eTarget = nativeEvent.getEventTarget();
				if (Element.is(eTarget)) {
					Element e = Element.as(eTarget);
					if(blur||focus){
						String tag=e.getTagName().toLowerCase();
						if(tag.equals("input")&&e.getAttribute("type").equals("button")){
							return;
						}
						if(!(tag.equals("input")||tag.equals("select")||tag.equals("textarea"))){
							return;
						}
					}
					List<String> tags = new ArrayList<String>();
					String text = "";
					ClientNodeIterator itr = new ClientNodeIterator(e,
							ClientNodeIterator.SHOW_TEXT);
					itr.nextNode();
					while (text.length() < 50 && itr.getCurrentNode() != null) {
						Text t = (Text) itr.getCurrentNode();
						text += TextUtils.normaliseAndTrim(t.getData());
						itr.nextNode();
					}
					while (e != null) {
						List<String> parts = new ArrayList<String>();
						parts.add(e.getTagName());
						if (!e.getId().isEmpty()) {
							parts.add("#" + e.getId());
						}
						String cn = getClassName(e);
						if (!cn.isEmpty()) {
							parts.add("." + cn);
						}
						tags.add(CommonUtils.join(parts, ""));
						if(e.getParentElement()==null&&!e.getTagName().equals("HTML")){
							//probably doing something drastic in a previous native handler - try to defer
						}
						e = e.getParentElement();
					}
					Collections.reverse(tags);
					String path = CommonUtils.join(tags, "/");
					String valueMessage="";
					if(blur||focus){
						String value = Element.as(eTarget).getPropertyString("value");
						String ih=Element.as(eTarget).getInnerHTML();
						valueMessage=CommonUtils.formatJ("%s%s",ClientLogRecord.VALUE_SEPARATOR,value);
						if(focus){
							lastFocussedValueMessage=valueMessage;
							return;
						}else{
							if(valueMessage.equals(lastFocussedValueMessage)){
								lastFocussedValueMessage=null;
								return;//no change
							}
						}
					}
					AlcinaTopics.logCategorisedMessage(new StringPair(
							click ? AlcinaTopics.LOG_CATEGORY_CLICK
									: AlcinaTopics.LOG_CATEGORY_CHANGE, CommonUtils
									.formatJ("%s :: [%s]%s", path, text,valueMessage)));
				}
			}
		}
		final native String getClassName(Element elt) /*-{
			var cn = elt.className;
			//note - someone says IE DOM objects don't support - hence try/catch
			try {
				if (cn.hasOwnProperty("baseVal")) {
					cn = cn.baseVal;
				}
				if ((typeof cn).toLowerCase() != "string") {
					debugger;
				}
			} catch (e) {
				return "";
			}
			return cn;
		}-*/;

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

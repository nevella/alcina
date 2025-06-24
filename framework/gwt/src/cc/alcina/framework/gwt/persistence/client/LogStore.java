package cc.alcina.framework.gwt.persistence.client;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.logic.domain.EntityHelper;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.PlaintextProtocolHandler;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.PlaintextProtocolHandlerShort;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.res.AlcinaProperties;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.gwt.client.util.EventCollator;
import cc.alcina.framework.gwt.client.util.Lzw;

/**
 * At the moment:
 * <ul>
 * <li>perist recent logs immediately (1K limit) to a cookie
 * <li>persist in 1s/30kb chunks to webdb
 * <li>persist in 30s/30kb chunks to remote
 * </ul>
 * <p>
 * This mechanism is app restart, offline and (almost) crash proof <br>
 * </p>
 *
 * 
 *
 */
@Registration.Singleton
public class LogStore {
	public static final String STORAGE_COOKIE_KEY = LogStore.class.getName()
			+ ".CookieStorage";

	public static final String DEFAULT_TABLE_NAME = "LogStore";

	public static final Topic<Void> topicDeleted = Topic.create();

	public static final Topic<IntPair> topicPersisted = Topic.create();

	public static final Topic<ClientLogRecord> topicEventOccurred = Topic
			.create();

	public static LogStore get() {
		return Registry.impl(LogStore.class);
	}

	private RemoteLogPersister remoteLogPersister;

	private ClientLogRecords logs = new ClientLogRecords();

	// persist logs to local db
	private EventCollator localPersistenceTimer = new EventCollator(1000,
			new Runnable() {
				@Override
				public void run() {
					flushToLocalPersistence();
				}
			});

	// push logs to remote store
	private EventCollator remotePersistenceTimer = new EventCollator(20000,
			new Runnable() {
				@Override
				public void run() {
					if (!isLocalPersistencePaused()) {
						pushLogsToRemote();
					}
				}
			});

	private String lastTopic;

	private AsyncCallback<Integer> afterLocalPersistence = new AsyncCallback<Integer>() {
		@Override
		public void onFailure(Throwable caught) {
			AlcinaTopics.muteStatisticsLogging.publish(false);
			AlcinaTopics.localPersistenceException.publish(caught);
			// in general, squelch
		}

		@Override
		public void onSuccess(Integer result) {
			AlcinaTopics.muteStatisticsLogging.publish(false);
			remotePersistenceTimer.eventOccurred();
		}
	};

	private int localSeriesIdCounter = 0;

	private boolean usesLzw;

	private boolean muted;

	private TopicListener<StringPair> stringPairListener = message -> log(
			message.s1, message.s2);

	private String lastMessage;

	public boolean useCookieMsgBackup = true;

	private int lastCookieId;

	protected PersistenceObjectStore objectStore;

	private boolean localPersistencePaused;

	private boolean emittedLogStoreException = false;

	public LogStore() {
		if (!AlcinaProperties.is(AlcinaProperties.class,
				AlcinaProperties.NON_BROWSER)) {
			String cookie = Cookies.getCookie(STORAGE_COOKIE_KEY);
			if (cookie != null) {
				try {
					log("restart", cookie);
				} catch (Exception e) {
					// probably module system not initialised
				}
			}
		}
	}

	public void add(String key, final String value,
			final AsyncCallback<Integer> idCallback) {
		this.objectStore.add(key, value, new AsyncCallback<Integer>() {
			@Override
			public void onFailure(Throwable caught) {
				idCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(Integer result) {
				topicPersisted.publish(new IntPair(result, value.length()));
				idCallback.onSuccess(result);
			}
		});
	}

	public String dumpLogsAsString() {
		if (objectStore != null && objectStore instanceof SyncObjectStore) {
			flushToLocalPersistence();
			return ((SyncObjectStore) objectStore).dumpValuesAsStringList();
		} else {
			return "Incorrect object store type for dump";
		}
	}

	public void flushToLocalPersistence() {
		if (logs.size > 0 && this.objectStore != null
				&& !isLocalPersistencePaused()) {
			String serialized = TransformManager.serialize(logs);
			if (isUsesLzw()) {
				setMuted(true);
				try {
					// unfortunately, have to encode to base64 here - unless we
					// want to be trixy with SQLLite
					String maybeShorter = "lzwb:" + Base64Utils.toBase64(
							new Lzw().compress(serialized).getBytes("UTF-8"));
					if (maybeShorter.length() < serialized.length()) {
						if (!GWT.isScript()) {
							locallyPersistLogs(serialized);
						}
						serialized = maybeShorter;
					}
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
				setMuted(false);
			}
			logs = new ClientLogRecords();
			locallyPersistLogs(serialized);
		}
	}

	public void getIdRange(AsyncCallback<IntPair> completedCallback) {
		this.objectStore.getIdRange(completedCallback);
	}

	int getLocalSeriesIdCounter() {
		return this.localSeriesIdCounter;
	}

	public ClientLogRecords getLogs() {
		return this.logs;
	}

	public void getRange(int fromId, int toId,
			AsyncCallback<Map<Integer, String>> valueCallback) {
		this.objectStore.getRange(fromId, toId, valueCallback);
	}

	public RemoteLogPersister getRemoteLogPersister() {
		return this.remoteLogPersister;
	}

	public TopicListener<StringPair> getStringPairListener() {
		return stringPairListener;
	}

	public String getTableName() {
		return objectStore.getTableName();
	}

	public boolean isLocalPersistencePaused() {
		return this.localPersistencePaused;
	}

	public boolean isMuted() {
		return this.muted;
	}

	public boolean isUsesLzw() {
		return this.usesLzw;
	}

	public void locallyPersistLogs(String serialized) {
		if (useCookieMsgBackup && lastCookieId == localSeriesIdCounter) {
			Cookies.removeCookie(STORAGE_COOKIE_KEY);
		}
		AlcinaTopics.muteStatisticsLogging.publish(true);
		add(lastTopic, serialized, afterLocalPersistence);
	}

	public void log(String topic, String message) {
		try {
			log0(topic, message);
		} catch (Exception e) {
			if (!GWT.isScript()) {
				e.printStackTrace();
			} else {
				if (!emittedLogStoreException) {
					emittedLogStoreException = true;
					throw new RuntimeException(e);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void log0(String topic, String message) {
		if (CommonUtils.equalsWithNullEquality(message, lastMessage) || muted) {
			return;
		}
		if (!Client.Init.isComplete()) {
			//
			// Ax.out("Before reflection: \n%s\n%s\n", topic, message);
			return;
		}
		if (topic.equals(AlcinaTopics.LOG_CATEGORY_TRANSFORM)) {
			String protocol = LocalTransformPersistence.get()
					.getSerializationPolicy().getTransformPersistenceProtocol();
			if (protocol.equals(PlaintextProtocolHandler.VERSION)) {
				List<DomainTransformEvent> events = new PlaintextProtocolHandler()
						.deserialize(message);
				message = new PlaintextProtocolHandlerShort().serialize(events);
			}
		}
		this.lastMessage = message;
		this.lastTopic = topic;
		ClientInstance clientInstance = Permissions.get().getClientInstance();
		String clientInstanceAuth = clientInstance == null ? "(before cli)"
				: String.valueOf(clientInstance.getAuth());
		ClientLogRecord logRecord = new ClientLogRecord(++localSeriesIdCounter,
				clientInstanceAuth, EntityHelper.getIdOrZero(clientInstance),
				new Date(), topic, message, null);
		logs.addLogRecord(logRecord);
		if (useCookieMsgBackup) {
			String value = logs.buf
					.substring(Math.max(logs.buf.length() - 1000, 0));
			Date d = new Date();
			d.setYear(d.getYear() + 5);
			Cookies.setCookie(STORAGE_COOKIE_KEY, value, d);
			lastCookieId = localSeriesIdCounter;
		}
		if (logs.size > RemoteLogPersister.PREFERRED_MAX_PUSH_SIZE) {
			flushToLocalPersistence();
		} else {
			localPersistenceTimer.eventOccurred();
		}
		topicEventOccurred.publish(logRecord);
	}

	public void pushLogsToRemote() {
		if (remoteLogPersister != null) {
			remoteLogPersister.push();
		}
	}

	public void registerDelegate(PersistenceObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	public void removeIdRange(IntPair range,
			final AsyncCallback<Void> completedCallback) {
		this.objectStore.removeIdRange(range, new AsyncCallback<Void>() {
			@Override
			public void onFailure(Throwable caught) {
				completedCallback.onFailure(caught);
			}

			@Override
			public void onSuccess(Void result) {
				topicDeleted.signal();
				completedCallback.onSuccess(result);
			}
		});
	}

	public void setLocalPersistencePaused(boolean localPersistencePaused) {
		this.localPersistencePaused = localPersistencePaused;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	public void setRemoteLogPersister(RemoteLogPersister remoteLogPersister) {
		this.remoteLogPersister = remoteLogPersister;
	}

	public void setUsesLzw(boolean usesLzw) {
		this.usesLzw = usesLzw;
	}
}

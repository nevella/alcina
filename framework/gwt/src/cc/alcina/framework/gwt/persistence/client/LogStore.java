package cc.alcina.framework.gwt.persistence.client;

import java.util.Date;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;
import cc.alcina.framework.gwt.client.util.Base64Utils;
import cc.alcina.framework.gwt.client.util.Lzw;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Cookies;

/**
 * At the moment:
 * <ul>
 * <li>perist recent logs immediately (1K limit) to a cookie
 * <li>
 * persist in 1s/30kb chunks to webdb
 * <li>persist in 30s/30kb chunks to remote
 * </ul>
 * <p>
 * This mechanism is app restart, offline and (almost) crash proof <br>
 * TODO - make the 'third layer' (cookie) backup mechanism pluggable - use
 * localstorage where possible
 * </p>
 * 
 * @author nick@alcina.cc
 * 
 */
public class LogStore {
	private static LogStore theInstance;

	public static LogStore get() {
		if (theInstance == null) {
			theInstance = new LogStore();
		}
		return theInstance;
	}

	private RemoteLogPersister remoteLogPersister;

	private ClientLogRecords logs = new ClientLogRecords();

	// persist logs to local db
	private AtEndOfEventSeriesTimer localPersistenceTimer = new AtEndOfEventSeriesTimer(
			1000, new Runnable() {
				@Override
				public void run() {
					flushToLocalPersistence();
				}
			});

	// push logs to remote store
	private AtEndOfEventSeriesTimer remotePersistenceTimer = new AtEndOfEventSeriesTimer(
			20000, new Runnable() {
				@Override
				public void run() {
					pushLogsToRemote();
				}
			});

	private String lastTopic;

	private PersistenceCallback<Integer> afterLocalPersistence = new PersistenceCallback<Integer>() {
		@Override
		public void onFailure(Throwable caught) {
			AlcinaTopics.muteStatisticsLogging(false);
			AlcinaTopics.localPersistenceException(caught);
			// in general, squelch
		}

		@Override
		public void onSuccess(Integer result) {
			AlcinaTopics.muteStatisticsLogging(false);
			remotePersistenceTimer.triggerEventOccurred();
		}
	};

	private int localSeriesIdCounter = 0;

	private boolean usesLzw;

	private boolean muted;

	private TopicListener<StringPair> stringPairListener = new TopicListener<StringPair>() {
		@Override
		public void topicPublished(String key, StringPair message) {
			log(message.s1, message.s2);
		}
	};

	private String lastMessage;

	public boolean useCookieMsgBackup = true;

	public static final String STORAGE_COOKIE_KEY = LogStore.class.getName()
			+ ".CookieStorage";

	private int lastCookieId;

	protected ObjectStore objectStore;

	protected LogStore() {
		String cookie = Cookies.getCookie(STORAGE_COOKIE_KEY);
		if (cookie != null) {
			log("restart", cookie);
		}
	}

	public void add(String key, String value,
			PersistenceCallback<Integer> idCallback) {
		this.objectStore.add(key, value, idCallback);
	}

	public void getIdRange(PersistenceCallback<IntPair> completedCallback) {
		this.objectStore.getIdRange(completedCallback);
	}

	public void getRange(int fromId, int toId,
			PersistenceCallback<Map<Integer, String>> valueCallback) {
		this.objectStore.getRange(fromId, toId, valueCallback);
	}

	public RemoteLogPersister getRemoteLogPersister() {
		return this.remoteLogPersister;
	}

	public TopicListener<StringPair> getStringPairListener() {
		return stringPairListener;
	}

	public void locallyPersistLogs(String serialized) {
		if (useCookieMsgBackup && lastCookieId == localSeriesIdCounter) {
			Cookies.removeCookie(STORAGE_COOKIE_KEY);
		}
		AlcinaTopics.muteStatisticsLogging(true);
		add(lastTopic, serialized, afterLocalPersistence);
	}

	@SuppressWarnings("deprecation")
	public void log(String topic, String message) {
		if (CommonUtils.equalsWithNullEquality(message, lastMessage) || muted) {
			return;
		}
		this.lastMessage = message;
		this.lastTopic = topic;
		ClientInstance cli = ClientLayerLocator.get().getClientInstance();
		String clientInstanceAuth = cli == null ? "(before cli)" : String
				.valueOf(cli.getAuth());
		ClientLogRecord logRecord = new ClientLogRecord(++localSeriesIdCounter,
				clientInstanceAuth, HiliHelper.getIdOrZero(cli), new Date(),
				topic, message,null);
		logs.addLogRecord(logRecord);
		if (useCookieMsgBackup) {
			String value = logs.buf.substring(Math.max(
					logs.buf.length() - 1000, 0));
			Date d = new Date();
			d.setYear(d.getYear() + 5);
			Cookies.setCookie(STORAGE_COOKIE_KEY, value, d);
			lastCookieId = localSeriesIdCounter;
		}
		if (logs.size > RemoteLogPersister.PREFERRED_MAX_PUSH_SIZE) {
			flushToLocalPersistence();
		} else {
			localPersistenceTimer.triggerEventOccurred();
		}
	}

	public void pushLogsToRemote() {
		if (remoteLogPersister != null) {
			remoteLogPersister.push();
		}
	}

	public void registerDelegate(ObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	public void removeIdRange(IntPair range,
			PersistenceCallback<Void> completedCallback) {
		this.objectStore.removeIdRange(range, completedCallback);
	}

	public void setRemoteLogPersister(RemoteLogPersister remoteLogPersister) {
		this.remoteLogPersister = remoteLogPersister;
	}

	public void flushToLocalPersistence() {
		if (logs.size > 0 && this.objectStore != null) {
			String serialized = new AlcinaBeanSerializer().serialize(logs);
			if (isUsesLzw()) {
				setMuted(true);
				try {
					// unfortunately, have to encode to base64 here - unless we
					// want to be trixy with SQLLite
					String maybeShorter = "lzwb:"
							+ Base64Utils.toBase64(new Lzw().compress(
									serialized).getBytes("UTF-8"));
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

	public String dumpLogsAsString() {
		if (objectStore != null && objectStore instanceof SyncObjectStore) {
			flushToLocalPersistence();
			return ((SyncObjectStore) objectStore).dumpValuesAsStringList();
		} else {
			return "Incorrect object store type for dump";
		}
	}

	public boolean isUsesLzw() {
		return this.usesLzw;
	}

	public void setUsesLzw(boolean usesLzw) {
		this.usesLzw = usesLzw;
	}

	public boolean isMuted() {
		return this.muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}
}

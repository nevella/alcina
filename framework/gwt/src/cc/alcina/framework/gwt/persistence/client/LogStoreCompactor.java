package cc.alcina.framework.gwt.persistence.client;

import java.util.Map;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecordKeepNonCriticalPrecedingContextFilter;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecordIsNonCriticalFilter;
import cc.alcina.framework.common.client.entity.ClientLogRecord.ClientLogRecords;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.state.LoopingPlayer;
import cc.alcina.framework.common.client.state.EnumPlayer.EnumRunnableAsyncCallbackPlayer;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.persistence.client.LogStoreCompactor.Phase;

public class LogStoreCompactor extends Consort<Phase, Object> {
	private int minNonCompactedLogRecordId = -1;

	private TopicListener<IntPair> logPersistedListener = new TopicListener<IntPair>() {
		@Override
		public void topicPublished(String key, IntPair message) {
			if (PermissionsManager.isOffline() && !isRunning()) {
				LogStore.get().setLocalPersistencePaused(true);
				restart();
			}
		}
	};

	public IntPair currentIdRange;

	ClientLogRecords mergeTo = null;

	public int mergeFromId;

	private ClientLogRecords mergeFrom;

	private ClientLogRecords mergeCheck;

	public int mergeCheckId;

	private TopicListener consortEndListener = new TopicListener() {
		@Override
		public void topicPublished(String key, Object message) {
			LogStore.get().setLocalPersistencePaused(false);
		}
	};

	public LogStoreCompactor() {
		addPlayer(new Compactor_GET_ID_RANGE());
		addPlayer(new Compactor_GET_RECORDS_AFTER_MERGE_SOURCE_TO_CHECK_FOR_EXCEPTION());
		addPlayer(new Compactor_GET_UNCOMPACTED_MERGE_SOURCE());
		addPlayer(new Compactor_GET_UNCOMPACTED_MERGE_TARGET());
		addPlayer(new Compactor_MERGE_AND_PERSIST_FROM());
		addPlayer(new Compactor_MERGE_AND_PERSIST_TO());
		addEndpointPlayer();
		setTrace(true);
		listenerDelta(FINISHED, consortEndListener, true);
		listenerDelta(ERROR, consortEndListener, true);
	}

	public void install() {
		LogStore.notifyPersistedListenerDelta(logPersistedListener, true);
	}

	public boolean isCompacted(ClientLogRecords records) {
		records.recalcSize();
		boolean hasNonCritical = CollectionFilters.first(
				records.getLogRecords(),
				new ClientLogRecordIsNonCriticalFilter()) != null;
		boolean hasException = CollectionFilters.first(records.getLogRecords(),
				new ClientLogRecordKeepNonCriticalPrecedingContextFilter()) != null;
		return (hasException || !hasNonCritical)
				&& records.size > RemoteLogPersister.PREFERRED_MAX_PUSH_SIZE;
	}

	class Compactor_GET_ID_RANGE extends
			EnumRunnableAsyncCallbackPlayer<IntPair, Phase> {
		public Compactor_GET_ID_RANGE() {
			super(Phase.GOT_ID_RANGE);
		}

		@Override
		public void onSuccess(IntPair result) {
			currentIdRange = result;
			if (result.isPoint()) {
				// 1 record? exit
				finished("only one log record");
				return;
			}
			minNonCompactedLogRecordId = currentIdRange
					.trimToRange(minNonCompactedLogRecordId);
			super.onSuccess(result);
		}

		@Override
		public void run() {
			currentIdRange = null;
			mergeCheckId = -1;
			mergeCheck = null;
			mergeFrom = null;
			mergeFromId = -1;
			LogStore.get().getIdRange(this);
		}
	}

	class Compactor_GET_RECORDS_AFTER_MERGE_SOURCE_TO_CHECK_FOR_EXCEPTION
			extends Compactor_GET_UNCOMPACTED_RECORD {
		public Compactor_GET_RECORDS_AFTER_MERGE_SOURCE_TO_CHECK_FOR_EXCEPTION() {
			super(Phase.GOT_RECORDS_AFTER_MERGE_SOURCE_TO_CHECK_FOR_EXCEPTION);
		}

		protected boolean continueIfCompacted() {
			return false;
		}

		@Override
		protected void eof() {
			wasPlayed();
		}

		@Override
		void foundUnMerged(ClientLogRecords records) {
			mergeCheck = records;
			mergeCheckId = recordId;
		}

		@Override
		protected int getInitialMinimumRecordId() {
			return mergeFromId + 1;
		}
	}

	class Compactor_GET_UNCOMPACTED_MERGE_SOURCE extends
			Compactor_GET_UNCOMPACTED_RECORD {
		public Compactor_GET_UNCOMPACTED_MERGE_SOURCE() {
			super(Phase.GOT_UNCOMPACTED_MERGE_SOURCE);
		}

		@Override
		void foundUnMerged(ClientLogRecords records) {
			mergeFrom = records;
			mergeFromId = recordId;
		}

		@Override
		protected int getInitialMinimumRecordId() {
			return minNonCompactedLogRecordId + 1;
		}
	}

	class Compactor_GET_UNCOMPACTED_MERGE_TARGET extends
			Compactor_GET_UNCOMPACTED_RECORD {
		public Compactor_GET_UNCOMPACTED_MERGE_TARGET() {
			super(Phase.GOT_UNCOMPACTED_MERGE_TARGET);
		}

		@Override
		void foundUnMerged(ClientLogRecords records) {
			mergeTo = records;
			minNonCompactedLogRecordId = recordId;
		}

		@Override
		protected int getInitialMinimumRecordId() {
			return minNonCompactedLogRecordId;
		}
	}

	abstract class Compactor_GET_UNCOMPACTED_RECORD extends
			EnumRunnableAsyncCallbackPlayer<Map<Integer, String>, Phase>
			implements LoopingPlayer {
		int recordId = -1;

		public void loop() {
			LogStore.get().getRange(recordId, recordId, this);
		}

		@Override
		public void setConsort(Consort<Phase, ?> consort) {
			super.setConsort(consort);
		}

		public Compactor_GET_UNCOMPACTED_RECORD(Phase phase) {
			super(phase);
		}

		@Override
		public String describeLoop() {
			return "increment until we find a compactable or otherwise worthwhile record chunk";
		}

		protected abstract int getInitialMinimumRecordId();

		@Override
		public void onSuccess(Map<Integer, String> result) {
			if (result.isEmpty()) {
				if (recordId >= currentIdRange.i2) {
					eof();
					return;
				} else {
					recordId++;
					replay(this);
					return;
				}
			}
			try {
				ClientLogRecords records = new AlcinaBeanSerializer()
						.deserialize(result.values().iterator().next());
				if (isCompacted(records) && continueIfCompacted()) {
					recordId++;
					replay(this);
					return;
				} else {
					foundUnMerged(records);
					super.onSuccess(result);
					return;
				}
			} catch (Exception e) {
				recordId++;
				consort.onFailure(e);
				return;
			}
		}

		protected boolean continueIfCompacted() {
			return true;
		}

		@Override
		public void run() {
			recordId = getInitialMinimumRecordId();
			loop();
		}

		protected void eof() {
			finished("eof");
		}

		abstract void foundUnMerged(ClientLogRecords records);
	}

	void finished(String cause) {
		System.out.println(cause);
		finished();
	}

	class Compactor_MERGE_AND_PERSIST_FROM extends
			EnumRunnableAsyncCallbackPlayer<Void, Phase> {
		public Compactor_MERGE_AND_PERSIST_FROM() {
			super(Phase.MERGED_AND_PERSISTED_FROM);
		}

		@Override
		public void run() {
			if (mergeTo == null || mergeFrom == null) {
				finished("nothing to merge");
			}
			if (mergeCheck != null) {
				boolean hasExceptionKeepContext = CollectionFilters
						.first(mergeCheck.getLogRecords(),
								new ClientLogRecordKeepNonCriticalPrecedingContextFilter()) != null;
				if (hasExceptionKeepContext) {
					minNonCompactedLogRecordId = mergeCheckId + 1;
					restart();
					return;
				}
			}
			CollectionFilters.filterInPlace(mergeTo.getLogRecords(),
					CollectionFilters
							.inverse(new ClientLogRecordIsNonCriticalFilter()));
			CollectionFilters.filterInPlace(mergeFrom.getLogRecords(),
					CollectionFilters
							.inverse(new ClientLogRecordIsNonCriticalFilter()));
			while (!mergeFrom.getLogRecords().isEmpty()
					&& !isCompacted(mergeTo)) {
				mergeTo.addLogRecord(mergeFrom.getLogRecords().remove(0));
			}
			String serialized = new AlcinaBeanSerializer().serialize(mergeTo);
			LogStore.get().objectStore.put(minNonCompactedLogRecordId,
					serialized, this);
		}
	}

	class Compactor_MERGE_AND_PERSIST_TO extends
			EnumRunnableAsyncCallbackPlayer<Void, Phase> {
		public Compactor_MERGE_AND_PERSIST_TO() {
			super(Phase.MERGED_AND_PERSISTED_TO);
		}

		@Override
		public void run() {
			if (mergeFrom.getLogRecords().isEmpty()) {
				LogStore.get().objectStore.removeIdRange(
						IntPair.point(mergeFromId), this);
			} else {
				String serialized = new AlcinaBeanSerializer()
						.serialize(mergeFrom);
				LogStore.get().objectStore.put(mergeFromId, serialized, this);
			}
		}
	}

	enum Phase {
		GOT_ID_RANGE, GOT_UNCOMPACTED_MERGE_TARGET,
		GOT_UNCOMPACTED_MERGE_SOURCE,
		GOT_RECORDS_AFTER_MERGE_SOURCE_TO_CHECK_FOR_EXCEPTION,
		MERGED_AND_PERSISTED_FROM, MERGED_AND_PERSISTED_TO
	}
}

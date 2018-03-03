package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.servlet.sync.SyncItemMatch.SyncItemLogType;

public class SyncItemMatch<T> {
	public enum SyncItemLogType {
		MERGE, PERSIST, MATCH
	}

	public static class SyncItemLogRecord {
		public SyncItemLogType type;

		public String message;

		public SyncItemLogRecord() {
		}

		public SyncItemLogRecord(SyncItemLogType type, String message) {
			super();
			this.type = type;
			this.message = message;
		}
	}

	public T left;

	public T right;

	public boolean ambiguous;

	public List<SyncItemLogRecord> logs = new ArrayList<>();

	public void log(SyncItemLogType type, String message) {
		logs.add(new SyncItemLogRecord(type, message));
	}

	public void logMerge(String message) {
		log(SyncItemLogType.MERGE, message);
	}
	public void logMatch(String message) {
		log(SyncItemLogType.MATCH, message);
	}

	public String logSummary() {
		return logs.stream().map(r -> r.message)
				.collect(Collectors.joining(" :: "));
	}

	public String typedLog(SyncItemLogType type) {
		return logs.stream().filter(r -> r.type.equals(type))
				.map(r -> r.message).findFirst().orElse("---");
	}
}
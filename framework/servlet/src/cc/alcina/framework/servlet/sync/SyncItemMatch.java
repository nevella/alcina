package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Ax;

public class SyncItemMatch<T> {
    public T left;

    public T right;

    public boolean ambiguous;

    public List<SyncItemLogRecord> logs = new ArrayList<>();

    public SyncItemLogStatus currentSyncStatus = SyncItemLogStatus.UNSYNCED;

    public SyncItemIssueType issueType = null;

    public String extId = "";

    public String extSource = "";

    public Date date;

    public String issue;

    public String extLink;

    public void log(SyncItemLogType type, String message) {
        logs.add(new SyncItemLogRecord(type, message));
    }

    public void logMatch(String message) {
        log(SyncItemLogType.MATCH, message);
    }

    public void logMerge(String message) {
        log(SyncItemLogType.MERGE, message);
    }

    public String logSummary() {
        String summary = logs.stream().map(r -> r.message)
                .collect(Collectors.joining(" :: "));
        if (issue != null) {
            summary += Ax.format(" ** Issue: %s", issue);
        }
        return summary;
    }

    public String typedLog(SyncItemLogType type) {
        return logs.stream().filter(r -> r.type.equals(type))
                .map(r -> r.message).findFirst().orElse("---");
    }

    public enum SyncItemIssueType {
        DATE_MISMATCH, NO_TAX_TYPE
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

    public enum SyncItemLogStatus {
        SYNCED, UNSYNCED, CATEGORY_IGNORED, CATEGORY_CUSTOM_IGNORED
    }

    public enum SyncItemLogType {
        MERGE, PERSIST, MATCH
    }
}
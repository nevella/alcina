package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.servlet.sync.SyncItemMatch.SyncItemLogStatus;

public class SyncLogger {
	public List<SyncLoggerRow> rows = new ArrayList<>();

	public void log(SyncItemMatch itemMatch, SyncPair pair) {
		rows.add(new SyncLoggerRow(itemMatch, pair));
	}

	public static class SyncLoggerRow {
		public SyncItemMatch itemMatch;

		public SyncPair pair;

		public SyncLoggerRow() {
		}

		public SyncLoggerRow(SyncItemMatch itemMatch, SyncPair pair) {
			this.itemMatch = itemMatch;
			this.pair = pair;
		}

		public boolean provideHadIssue(boolean ambiguousOk) {
			boolean hadIssue = itemMatch.currentSyncStatus == SyncItemLogStatus.UNSYNCED
					&& ((itemMatch.ambiguous && !ambiguousOk)
							|| itemMatch.issue != null);
			return hadIssue;
		}
	}
}

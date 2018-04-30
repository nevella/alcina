package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.servlet.sync.SyncItemMatch.SyncItemLogStatus;
import cc.alcina.framework.servlet.sync.SyncLogger.SyncLoggerRow;

public class SyncLogger {
	public static class SyncLoggerRow {
		public SyncItemMatch itemMatch;

		public SyncPair pair;

		public SyncLoggerRow() {
		}

		public SyncLoggerRow(SyncItemMatch itemMatch, SyncPair pair) {
			this.itemMatch = itemMatch;
			this.pair = pair;
		}

		public boolean provideHadIssue() {
			boolean hadIssue = itemMatch.currentSyncStatus == SyncItemLogStatus.UNSYNCED
					&& (itemMatch.ambiguous || itemMatch.issue != null);
			if(hadIssue){
				int debug=3;
			}
			return hadIssue;
		}
	}

	public List<SyncLoggerRow> rows = new ArrayList<>();

	public void log(SyncItemMatch itemMatch, SyncPair pair) {
		rows.add(new SyncLoggerRow(itemMatch, pair));
	}
}

package cc.alcina.framework.servlet.sync;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.process.ContextObservers;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.servlet.sync.SyncItemMatch.SyncItemLogStatus;

public class SyncLogger {
	public List<SyncLoggerRow> rows = new ArrayList<>();

	public void log(SyncItemMatch itemMatch, SyncPair pair) {
		rows.add(new SyncLoggerRow(itemMatch, pair));
		if (pair == null) {
			if (itemMatch.currentSyncStatus == SyncItemLogStatus.CATEGORY_CUSTOM_IGNORED
					|| itemMatch.currentSyncStatus == SyncItemLogStatus.CATEGORY_IGNORED) {
				new IgnoredObservable(itemMatch).publish();
			}
		}
	}

	public static class IgnoredObservable
			implements ContextObservers.Observable {
		public SyncItemMatch itemMatch;

		public IgnoredObservable(SyncItemMatch itemMatch) {
			this.itemMatch = itemMatch;
		}

		@Override
		public String toString() {
			return itemMatch.toString();
		}
	}

	public static class SyncLoggerRow {
		public SyncItemMatch itemMatch;

		public SyncPair pair;

		@Override
		public String toString() {
			return FormatBuilder.keyValues("itemMatch", itemMatch, "pair",
					pair);
		}

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

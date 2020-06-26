package cc.alcina.framework.servlet.sync;

public class FlatDeltaPersisterResult {
	public int createCount = 0;

	public int mergeCount = 0;

	public int noModificationCount = 0;

	public int deletionCount = 0;

	public int unMatchedCount = 0;

	public boolean allPersisted;

	public boolean mergeInterrupted;

	public String numbers() {
		return String.format(
				"created: %s - merged: %s - deleted:"
						+ " %s - unmodified: %s - unmatched: %s",
				createCount, mergeCount, deletionCount, noModificationCount,
				unMatchedCount);
	}

	@Override
	public String toString() {
		return numbers();
	}

	public void update(FlatDeltaPersisterResultType result) {
		switch (result) {
		case CREATED:
			createCount++;
			break;
		case MERGED:
			mergeCount++;
			break;
		case DELETED:
			deletionCount++;
			break;
		case UNMODIFIED:
			noModificationCount++;
			break;
		case UNMATCHED:
			unMatchedCount++;
			break;
		}
	}

	public boolean wasModified() {
		return createCount + mergeCount + deletionCount > 0;
	}

	public enum FlatDeltaPersisterResultType {
		CREATED, MERGED, DELETED, UNMODIFIED, UNMATCHED
	}
}
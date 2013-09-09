package cc.alcina.framework.servlet.sync;

public class FlatDeltaPersisterResult {
	public int createCount = 0;

	public int mergeCount = 0;

	public int noModificationCount = 0;

	public int deletionCount = 0;

	public boolean wasModified() {
		return createCount + mergeCount + deletionCount > 0;
	}
}
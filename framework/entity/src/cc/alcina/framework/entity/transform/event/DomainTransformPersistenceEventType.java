package cc.alcina.framework.entity.transform.event;

public enum DomainTransformPersistenceEventType {
	PREPARE_COMMIT,
	/**
	 * Careful! This one should probably only be used by framework code, app
	 * listeners should monitor {@link #PREPARE_COMMIT}
	 */
	PRE_COMMIT, COMMIT_OK, COMMIT_ERROR;

	boolean isPostCommit() {
		switch (this) {
		case COMMIT_ERROR:
		case COMMIT_OK:
			return true;
		default:
			return false;
		}
	}

	boolean isPrepareCommit() {
		switch (this) {
		case PREPARE_COMMIT:
			return true;
		default:
			return false;
		}
	}
}
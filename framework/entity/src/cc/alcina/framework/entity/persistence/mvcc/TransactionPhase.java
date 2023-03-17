package cc.alcina.framework.entity.persistence.mvcc;

enum TransactionPhase {
	TO_DB_PREPARING, TO_DB_PERSISTING, TO_DB_PERSISTED, TO_DB_ABORTED,
	TO_DOMAIN_PREPARING, TO_DOMAIN_COMMITTING, TO_DOMAIN_COMMITTED,
	TO_DOMAIN_ABORTED, VACUUM_BEGIN, VACUUM_ENDED, READ_ONLY,
	/**
	 * Entities can be created but not committed
	 */
	ADJUNCT;

	boolean isComplete() {
		switch (this) {
		case TO_DB_PERSISTED:
		case TO_DB_ABORTED:
		case TO_DOMAIN_COMMITTED:
		case TO_DOMAIN_ABORTED:
		case VACUUM_ENDED:
			return true;
		default:
			return false;
		}
	}

	boolean isDomain() {
		switch (this) {
		case TO_DOMAIN_COMMITTING:
		case TO_DOMAIN_COMMITTED:
			return true;
		default:
			return false;
		}
	}
}

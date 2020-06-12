package cc.alcina.framework.entity.entityaccess.cache.mvcc;

enum TransactionPhase {
	TO_DB_PREPARING, TO_DB_PERSISTING, TO_DB_PERSISTED, TO_DB_ABORTED,
	TO_DOMAIN_PREPARING, TO_DOMAIN_COMMITTING, TO_DOMAIN_COMMITTED,
	TO_DOMAIN_ABORTED, VACUUM_BEGIN, VACUUM_ENDED, NO_ACTIVE_TRANSACTION;
	public boolean isDomain() {
		switch (this) {
		case TO_DOMAIN_COMMITTING:
		case TO_DOMAIN_COMMITTED:
			return true;
		default:
			return false;
		}
	}

	public boolean isEnded() {
		switch (this) {
		case TO_DB_PERSISTED:
		case TO_DB_ABORTED:
		case TO_DOMAIN_COMMITTED:
		case TO_DOMAIN_ABORTED:
		case VACUUM_ENDED:
		case NO_ACTIVE_TRANSACTION:
			return true;
		default:
			return false;
		}
	}
}

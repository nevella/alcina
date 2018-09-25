package cc.alcina.framework.entity.entityaccess.cache;

public enum DomainCacheLockState {
	WAITING_FOR_LOCK, HOLDING_READ_LOCK, HOLDING_WRITE_LOCK, NO_LOCK;
}
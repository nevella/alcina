package cc.alcina.framework.entity.persistence.cache;

public enum DomainStoreLockAction {
	PRE_LOCK, MAIN_LOCK_ACQUIRED, SUB_LOCK_ACQUIRED, UNLOCK
}
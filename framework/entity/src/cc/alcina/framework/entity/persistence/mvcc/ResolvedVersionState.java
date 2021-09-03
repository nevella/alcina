package cc.alcina.framework.entity.persistence.mvcc;

public enum ResolvedVersionState {
	READ, WRITE,
	/*
	 * The state that property change listeners mutator calls resolve to - if
	 * read, it's logically incorrect but harmless - so warn or throw
	 */
	READ_INVALID
}

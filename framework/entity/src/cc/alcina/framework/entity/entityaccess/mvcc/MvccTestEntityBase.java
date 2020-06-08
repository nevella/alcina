package cc.alcina.framework.entity.entityaccess.mvcc;

import cc.alcina.framework.common.client.logic.domain.Entity;

public abstract class MvccTestEntityBase<T extends MvccTestEntityBase> extends Entity<T> {
	@SuppressWarnings("unused")
	private long invalidDuplicateFieldName;
}

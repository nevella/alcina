package cc.alcina.framework.entity.entityaccess.mvcc;

import java.util.stream.IntStream;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.MvccAccessCorrect;

public abstract class MvccTestEntityBase<T extends MvccTestEntityBase> extends Entity<T> {
	@SuppressWarnings("unused")
	private long invalidDuplicateFieldName;
}

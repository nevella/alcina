package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public abstract class MemCacheReader<I, O> {
	public O read(I input) {
		try {
			AlcinaMemCache.get().lock(false);
			return read0(input);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			AlcinaMemCache.get().unlock(false);
		}
	}

	protected abstract O read0(I input) throws Exception;
}

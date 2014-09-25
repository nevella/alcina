package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public abstract class MemCacheRunner extends MemCacheReader<Void, Void> {
	@Override
	protected Void read0(Void input) throws Exception {
		run();
		return null;
	}

	public MemCacheRunner() {
		try {
			read0(null);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected abstract void run() throws Exception;
}

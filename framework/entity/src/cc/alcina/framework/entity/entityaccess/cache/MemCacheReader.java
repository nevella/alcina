package cc.alcina.framework.entity.entityaccess.cache;

public abstract class MemCacheReader<I,O> {
	public  O read(I input){
		try {
			AlcinaMemCache.get().lock(false);
			return read0(input);
		} finally {
			AlcinaMemCache.get().unlock(false);
		}
	}

	protected abstract O read0(I input) ;
}

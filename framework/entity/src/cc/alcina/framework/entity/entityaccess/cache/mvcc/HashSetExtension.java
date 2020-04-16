package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.HashSet;

public class HashSetExtension<E> extends HashSet<E> {
	public static HashSetExtension debugInstance;

	@Override
	public boolean remove(Object o) {
		if (this == debugInstance) {
			int debug = 3;
		}
		return super.remove(o);
	}
}
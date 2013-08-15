package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Set;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

public class IdLookup<T, H extends HasIdAndLocalId> extends CacheLookup<T, H> {
	public IdLookup(CacheLookupDescriptor descriptor) {
		super(descriptor);
	}

	public void add(T k1, Long value) {
		if(k1==null){
			return;
		}
		Set<Long> set = getAndEnsure(k1);
		set.add(value);
		if (set.size() > 1) {
			// throw new IllegalArgumentException("");
			System.out
					.format("Warning - duplicate mapping of an id lookup - %s: %s : %s\n",
							this, k1, set);
		}
	}
	
	public H getObject(T key) {
		Set<Long> ids = get(key);
		if (ids != null) {
			Long id = CommonUtils.first(ids);
			return getForResolvedId(id);
		}
		return null;
	}
}

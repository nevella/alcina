package cc.alcina.framework.entity.entityaccess.cache;

import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

public class IdLookup<T, H extends HasIdAndLocalId> extends CacheLookup<T, H> {
	public IdLookup(CacheLookupDescriptor descriptor) {
		super(descriptor);
	}

	private Set<T> duplicateKeys = new LinkedHashSet<T>();

	public void add(T k1, Long value) {
		if (k1 == null) {
			return;
		}
		Set<Long> set = getAndEnsure(k1);
		set.add(value);
		if (set.size() > 1) {
			// throw new IllegalArgumentException("");
			Registry.impl(TaggedLoggers.class)
					.log(String.format(
							"Warning - duplicate mapping of an id lookup - %s: %s : %s\n",
							this, k1, set), AlcinaMemCache.class,
							TaggedLogger.WARN);
			duplicateKeys.add(k1);
		}
	}

	public boolean isUnique(T key) {
		return !duplicateKeys.contains(key);
	}

	public H getObject(T key) {
		H value = null;
		Set<Long> ids = get(key);
		if (ids != null) {
			Long id = CommonUtils.first(ids);
			value = getForResolvedId(id);
		}
		return AlcinaMemCache.get().transactional.resolveTransactional(this,
				value, new Object[] { key });
	}
}

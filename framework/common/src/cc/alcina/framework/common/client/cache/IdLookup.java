package cc.alcina.framework.common.client.cache;

import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

public class IdLookup<T, H extends HasIdAndLocalId> extends CacheLookup<T, H> {
	private Set<T> duplicateKeys = new LinkedHashSet<T>();

	public IdLookup(CacheLookupDescriptor descriptor, boolean concurrent) {
		super(descriptor, concurrent);
	}

	public H getObject(T key) {
		H value = null;
		Set<Long> ids = get(key);
		if (CommonUtils.isNotNullOrEmpty(ids)) {
			Long id = CommonUtils.first(ids);
			value = getForResolvedId(id);
		}
		return Domain.resolveTransactional(this, value, new Object[] { key });
	}

	public boolean isUnique(T key) {
		return !duplicateKeys.contains(key);
	}

	protected void add(T k1, Long value) {
		if (k1 == null) {
			return;
		}
		Set<Long> set = getAndEnsure(k1);
		set.add(value);
		if (set.size() > 1) {
			// throw new IllegalArgumentException("");
			Registry.impl(TaggedLoggers.class)
					.log(CommonUtils.formatJ(
							"Warning - duplicate mapping of an id lookup - %s: %s : %s\n",
							this, k1, set), Domain.class, TaggedLogger.WARN);
			duplicateKeys.add(k1);
		}
	}
}

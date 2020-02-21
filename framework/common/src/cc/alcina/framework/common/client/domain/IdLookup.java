package cc.alcina.framework.common.client.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class IdLookup<T, H extends HasIdAndLocalId> extends DomainLookup<T, H> {
	private Set<T> duplicateKeys = new LinkedHashSet<T>();

	protected final transient Logger logger = LoggerFactory
			.getLogger(getClass());

	public IdLookup(DomainStoreLookupDescriptor descriptor) {
		super(descriptor);
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

	@Override
	protected void add(T k1, Long value) {
		if (k1 == null) {
			return;
		}
		Set<Long> set = getAndEnsure(k1);
		set.add(value);
		if (set.size() > 1) {
			// throw new IllegalArgumentException("");
			logger.warn(Ax.format(
					"Warning - duplicate mapping of an id lookup - %s: %s : %s",
					this, k1, set));
			duplicateKeys.add(k1);
		}
	}
}

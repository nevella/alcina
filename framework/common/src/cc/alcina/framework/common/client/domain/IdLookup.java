package cc.alcina.framework.common.client.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class IdLookup<T, E extends Entity> extends DomainLookup<T, E> {
	private Set<T> duplicateKeys = new LinkedHashSet<T>();

	protected final transient Logger logger = LoggerFactory
			.getLogger(getClass());

	public IdLookup(DomainStoreLookupDescriptor descriptor) {
		super(descriptor);
	}

	public E getObject(T key) {
		E value = null;
		Set<E> values = get(key);
		if (CommonUtils.isNotNullOrEmpty(values)) {
			return values.iterator().next();
		} else {
			return null;
		}
	}

	public boolean isUnique(T key) {
		return !duplicateKeys.contains(key);
	}

	@Override
	protected void add(T key, E value) {
		if (key == null) {
			return;
		}
		super.add(key, value);
		Set<E> set = get(key);
		if (set.size() > 1) {
			// throw new IllegalArgumentException("");
			logger.warn(Ax.format(
					"Warning - duplicate mapping of an id lookup - %s: %s : %s",
					this, key, set));
			duplicateKeys.add(key);
		}
	}
}

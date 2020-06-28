package cc.alcina.framework.common.client.domain;

import java.util.Set;

import cc.alcina.framework.common.client.domain.FilterCost.HasFilterCost;
import cc.alcina.framework.common.client.logic.domain.Entity;

public interface IndexedValueProvider<E extends Entity> extends HasFilterCost {
	public Set<E> getKeyMayBeCollection(Object value);
}

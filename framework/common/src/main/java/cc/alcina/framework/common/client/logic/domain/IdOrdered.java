package cc.alcina.framework.common.client.logic.domain;

import cc.alcina.framework.common.client.util.CommonUtils;

public interface IdOrdered<T extends IdOrdered> extends Comparable<T> {
	long getId();

	@Override
	default int compareTo(T o) {
		return CommonUtils.compareLongs(getId(), o.getId());
	}
}
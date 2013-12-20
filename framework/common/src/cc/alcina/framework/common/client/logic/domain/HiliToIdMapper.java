package cc.alcina.framework.common.client.logic.domain;

import cc.alcina.framework.common.client.collections.FromObjectKeyValueMapper;

public class HiliToIdMapper extends
		FromObjectKeyValueMapper<Long, HasIdAndLocalId> {
	@Override
	public Long getKey(HasIdAndLocalId o) {
		return o.getId();
	}
}
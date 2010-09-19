package cc.alcina.framework.common.client.gwittir.validator;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface RequiresSourceValidator {
	public void setSourceObject(HasIdAndLocalId sourceObject
			);
}

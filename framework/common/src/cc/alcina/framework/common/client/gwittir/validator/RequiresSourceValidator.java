package cc.alcina.framework.common.client.gwittir.validator;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface RequiresSourceValidator<H extends HasIdAndLocalId> {
	public void setSourceObject(H sourceObject);
}

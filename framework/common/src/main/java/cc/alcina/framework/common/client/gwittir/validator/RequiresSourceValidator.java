package cc.alcina.framework.common.client.gwittir.validator;

import cc.alcina.framework.common.client.logic.domain.Entity;

public interface RequiresSourceValidator<H extends Entity> {
	public void setSourceObject(H sourceObject);
}

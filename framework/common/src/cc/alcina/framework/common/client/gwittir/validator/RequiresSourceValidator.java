package cc.alcina.framework.common.client.gwittir.validator;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.reflection.Property;

public interface RequiresSourceValidator<H extends Entity> {
	public void setSourceObject(H sourceObject);

	default void setOnProperty(Property property) {
	}
}

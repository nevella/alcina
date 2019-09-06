package cc.alcina.framework.gwt.client.data.entity;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;

public interface DataApiDecorators {
	@PropertyPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.ROOT))
	@Display(name = "Id", displayMask = Display.DISPLAY_RO_PROPERTY, styleName = "nowrap id", orderingHint = 5)
	public abstract long getId();
}

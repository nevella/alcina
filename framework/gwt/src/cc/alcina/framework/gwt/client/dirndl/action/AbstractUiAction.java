package cc.alcina.framework.gwt.client.dirndl.action;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.CommonUtils;

@Reflected
public abstract class AbstractUiAction<U extends AbstractUiAction>
		implements Permissible {
	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.EVERYONE;
	}

	public ActionIcon<U> asIconAction() {
		return new ActionIcon<U>((U) this);
	}

	public String getDescription() {
		return CommonUtils.deInfix(getClass().getSimpleName()).trim();
	}

	public String name() {
		return getClass().getSimpleName();
	}
}

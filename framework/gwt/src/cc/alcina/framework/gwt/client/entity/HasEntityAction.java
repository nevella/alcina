package cc.alcina.framework.gwt.client.entity;

import cc.alcina.framework.common.client.logic.domain.EntityAction;

public interface HasEntityAction {
	public EntityAction getAction();

	public void setAction(EntityAction action);
}

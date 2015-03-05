package cc.alcina.framework.entity.logic.permissions;

import cc.alcina.framework.common.client.logic.permissions.IUser;

public interface UserInstantiator {
	//note - don't require a full graph of instantiated, just the object itself
	public IUser instantiate(IUser user);
}

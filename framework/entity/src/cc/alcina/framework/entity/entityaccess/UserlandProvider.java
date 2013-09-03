package cc.alcina.framework.entity.entityaccess;

import cc.alcina.framework.common.client.logic.permissions.IUser;

public interface UserlandProvider {

	IUser getSystemUser(boolean clean);
}

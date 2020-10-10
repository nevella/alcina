package cc.alcina.framework.common.client.logic.domain;

import java.io.Serializable;

public interface UserPropertyPersistable extends Serializable {
	public UserProperty getUserProperty();

	public void setUserProperty(UserProperty userProperty);
}

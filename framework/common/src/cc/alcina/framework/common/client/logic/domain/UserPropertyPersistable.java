package cc.alcina.framework.common.client.logic.domain;

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;

public interface UserPropertyPersistable extends Serializable {
	@AlcinaTransient
	/*
	 * repeat the annotation in the implementation method
	 */
	public UserProperty getUserProperty();

	public void setUserProperty(UserProperty userProperty);
}

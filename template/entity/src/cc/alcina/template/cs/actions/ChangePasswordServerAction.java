package cc.alcina.template.cs.actions;

import java.io.Serializable;

import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.template.cs.csobjects.ChangePasswordModel;


public class ChangePasswordServerAction extends
		RemoteActionWithParameters<ChangePasswordModel> implements Serializable{
	public ChangePasswordServerAction() {
	}
	@Override
	public String getDescription() {
		return "Change password";
	}
	@Override
	public String getDisplayName() {
		return "Change password";
	}
	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.ADMIN_OR_OWNER;
	}
	
}

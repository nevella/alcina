package cc.alcina.template.servlet.actionhandlers;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.util.UnixCrypt;
import cc.alcina.framework.servlet.job.BaseRemoteActionPerformer;
import cc.alcina.template.cs.actions.ChangePasswordServerAction;
import cc.alcina.template.cs.csobjects.ChangePasswordModel;
import cc.alcina.template.cs.persistent.ActionLogItemImpl;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;

@RegistryLocation(registryPoint = RemoteActionPerformer.class, targetClass = ChangePasswordServerAction.class)
public class ChangePasswordActionServerHandler extends
		BaseRemoteActionPerformer<ChangePasswordServerAction> {
	private void performAction(ChangePasswordModel bindable) {
		jobStarted();
		CommonPersistenceLocal up = Registry.impl(
				CommonPersistenceProvider.class).getCommonPersistence();
		AlcinaTemplateUser user = up.getItemById(AlcinaTemplateUser.class,
				bindable.getUserId());
		Permissible p = new Permissible() {
			public String rule() {
				return null;
			}

			public AccessLevel accessLevel() {
				return AccessLevel.ADMIN_OR_OWNER;
			}
		};
		if (!PermissionsManager.get().isPermissible(user, p)) {
			throw new RuntimeException("Insufficient permissions");
		}
		String password = bindable.getNewPassword();
		if (CommonUtils.isNullOrEmpty(user.getSalt())) {
			user.setSalt(user.getUserName());
		}
		user.setPassword(UnixCrypt.crypt(user.getSalt(), password));
		user = (AlcinaTemplateUser) up.mergeUser(user);
		jobOk("Password changed for user " + user.getUserName());
	}

	public void performAction(ChangePasswordServerAction action) {
		performAction(action.getParameters());
	}
}

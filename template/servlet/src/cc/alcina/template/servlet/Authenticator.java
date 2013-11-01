package cc.alcina.template.servlet;

import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.util.UnixCrypt;
import cc.alcina.framework.servlet.authentication.AuthenticationException;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;

class Authenticator {
	protected AlcinaTemplateUser processAuthenticatedLogin(LoginResponse lrb,
			String userName) throws AuthenticationException {
		CommonPersistenceLocal up = Registry.impl(CommonPersistenceProvider.class).getCommonPersistence();
		AlcinaTemplateUser user = (AlcinaTemplateUser) up
				.getUserByName(userName);
		if (CommonUtils.bv(user.getDeleted())
				|| CommonUtils.bv(user.getSystem())) {
			lrb.setOk(false);
			lrb.setErrorMsg("User account disabled");
		}
		return user;
	}

	public IUser createUser(String userName, String password) {
		CommonPersistenceLocal up = Registry.impl(CommonPersistenceProvider.class).getCommonPersistence();
		try {
			ThreadedPermissionsManager.cast().pushSystemUser();
			AlcinaTemplateUser user = new AlcinaTemplateUser();
			user.setUserName(userName);
			user.setSalt(userName);
			user.setPassword(UnixCrypt.crypt(user.getSalt(), password));
			user = (AlcinaTemplateUser) up.mergeUser(user);
			((ThreadedPermissionsManager) PermissionsManager.get())
					.popSystemUser();
			return user;
		} finally {
			ThreadedPermissionsManager.cast().popSystemUser();
		}
	}

	public LoginResponse authenticate(LoginBean loginBean)
			throws AuthenticationException {
		LoginResponse lrb = new LoginResponse();
		lrb.setOk(false);
		String userName = loginBean.getUserName();
		CommonPersistenceLocal up = Registry.impl(CommonPersistenceProvider.class).getCommonPersistence();
		AlcinaTemplateUser user = (AlcinaTemplateUser) up
				.getUserByName(userName);
		if (user == null) {
			lrb.setErrorMsg("Email address not registered");
			return lrb;
		}
		if (user.getSalt() == null) {
			user.setSalt(user.getUserName());
			up.mergeUser(user);
		}
		if (!UnixCrypt.crypt(user.getSalt(), loginBean.getPassword()).equals(
				user.getPasswordHash())) {
			lrb.setErrorMsg("Password incorrect");
			return lrb;
		}
		lrb.setOk(true);
		if (lrb.isOk()) {
			processAuthenticatedLogin(lrb, loginBean.getUserName());
		}
		return lrb;
	}
}

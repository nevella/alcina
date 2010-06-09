package cc.alcina.template.cs.misc.search;

import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.search.AbstractUserCriteriaGroup;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;

@BeanInfo(displayNamePropertyName = "displayName")
@PermissibleChildClasses({UserCriterion.class})
public class UserCriteriaGroup extends AbstractUserCriteriaGroup<UserCriterion> {
	private transient boolean restrictToCurrentUser;

	public UserCriteriaGroup() {
		super();
		setDisplayName("User");
	}

	public void setRestrictToCurrentUser(boolean restrictToCurrentUser) {
		this.restrictToCurrentUser = restrictToCurrentUser;
		if (restrictToCurrentUser) {
			UserCriterion juc = (UserCriterion) getCriteria().iterator().next();
			juc
					.setUser((AlcinaTemplateUser) PermissionsManager.get()
							.getUser());
		}
	}

	public boolean isRestrictToCurrentUser() {
		return restrictToCurrentUser;
	}
}

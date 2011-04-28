package cc.alcina.template.cs.misc.search;

import cc.alcina.framework.common.client.logic.reflection.Association;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.common.client.search.AbstractDateCriterion;
import cc.alcina.framework.common.client.search.AbstractUserCriterion;
import cc.alcina.framework.common.client.search.EqlWithParameters;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.template.cs.persistent.AlcinaTemplateUser;

public class UserCriterion extends AbstractUserCriterion {
	static final transient long serialVersionUID = -1L;
	public UserCriterion() {
	}

	private Long userId;

	public Long getUserId() {
		return this.userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public UserCriterion(String displayName) {
		super(displayName);
	}

	private transient AlcinaTemplateUser user;

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (user == null) {
			return result;
		}
		result.eql = getTargetPropertyName() + ".id = ? ";
		result.parameters.add(getUserId());
		return result;
	}

	public boolean equivalentTo(SearchCriterion other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		UserCriterion otherImpl = (UserCriterion) other;
		return otherImpl.getDirection() == getDirection()
				&& CommonUtils.equalsWithNullEquality(getUserId(), otherImpl
						.getUserId());
	}

	@VisualiserInfo(displayInfo = @DisplayInfo(name = "AlcinaTemplate user", orderingHint = 10))
	@Association(implementationClass = AlcinaTemplateUser.class, propertyName = "")
	public AlcinaTemplateUser getUser() {
		return user;
	}

	public void setUser(AlcinaTemplateUser user) {
		AlcinaTemplateUser old_user = this.user;
		this.user = user;
		this.userId = user == null ? 0 : user.getId();
		propertyChangeSupport().firePropertyChange("user", old_user, user);
	}
}

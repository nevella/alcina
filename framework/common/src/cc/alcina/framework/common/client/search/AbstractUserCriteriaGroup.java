package cc.alcina.framework.common.client.search;

import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.CollectionModificationType;
import cc.alcina.framework.common.client.logic.permissions.Permissions;

public abstract class AbstractUserCriteriaGroup<SC extends AbstractUserCriterion>
		extends CriteriaGroup<SC> {
	@Override
	public void addCriterion(SC criterion) {
		Set<SC> deltaSet = TransformManager.getDeltaSet(getCriteria(),
				criterion, CollectionModificationType.ADD);
		setCriteria(deltaSet);
	}

	@Override
	public String validatePermissions() {
		String result = super.validatePermissions();
		if (result != null) {
			return result;
		}
		if (!Permissions.get().isAdmin()
				&& getCriteria().iterator().hasNext()) {
			AbstractUserCriterion uc = getCriteria().iterator().next();
			if (uc != null && uc.getUserId() != null && uc.getUserId()
					.longValue() != Permissions.get().getUserId()) {
				return "Access denied: not restricted to current user";
			}
		}
		return null;
	}
}

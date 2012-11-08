package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;

public abstract class AbstractUserCriteriaGroup<SC extends AbstractUserCriterion> extends CriteriaGroup<SC> {
	@Override
	public String validatePermissions() {
		 String result = super.validatePermissions();
		 if (result!=null){
			 return result;
		 }
		 if (!PermissionsManager.get().isAdmin()&&getCriteria().iterator().hasNext()){
			 AbstractUserCriterion uc = getCriteria().iterator().next();
			 if (uc!=null && uc.getUserId()!=null && uc.getUserId().longValue()!=PermissionsManager.get().getUserId()){
				return "Access denied: not restricted to current user"; 
			 }
		 }
		 return null;
	}
}

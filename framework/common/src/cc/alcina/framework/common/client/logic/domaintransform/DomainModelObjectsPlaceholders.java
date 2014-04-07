package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.gwt.client.data.GeneralProperties;

//ensure that domainmodeldelta will serialize
public class DomainModelObjectsPlaceholders {
	public static class DomainModelObjectsPlaceholder implements
			DomainModelObjects {
		@Override
		public Collection registrableObjects() {
			return null;
		}
	}

	public static class DomainModelHolderPlaceholder implements
			DomainModelHolder {
		@Override
		public List registerableDomainObjects() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Set<ClassRef> getClassRefs() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IUser getCurrentUser() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public GeneralProperties getGeneralProperties() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void registerSelfAsProvider() {
			// TODO Auto-generated method stub
		}
	}
}

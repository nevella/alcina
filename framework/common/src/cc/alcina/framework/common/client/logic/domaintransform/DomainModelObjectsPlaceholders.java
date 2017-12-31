package cc.alcina.framework.common.client.logic.domaintransform;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.gwt.client.data.GeneralProperties;

//ensure that domainmodeldelta will serialize
public class DomainModelObjectsPlaceholders {
	public static class DomainModelHolderPlaceholder
			implements DomainModelHolder {
		@Override
		public Set<ClassRef> getClassRefs() {
			return Collections.EMPTY_SET;
		}

		@Override
		public IUser getCurrentUser() {
			return null;
		}

		@Override
		public GeneralProperties getGeneralProperties() {
			return null;
		}

		@Override
		public List registerableDomainObjects() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public void registerSelfAsProvider() {
		}
	}

	public static class DomainModelObjectsPlaceholder
			implements DomainModelObject {
		@Override
		public void ensureLookups() {
		}

		@Override
		public Collection registrableObjects() {
			return null;
		}
	}
}

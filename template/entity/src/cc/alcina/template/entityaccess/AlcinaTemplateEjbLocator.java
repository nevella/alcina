package cc.alcina.template.entityaccess;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;

@RegistryLocation(registryPoint = CommonPersistenceProvider.class, implementationType = ImplementationType.SINGLETON)
public class AlcinaTemplateEjbLocator implements CommonPersistenceProvider {
	public CommonPersistenceLocal getCommonPersistence() {
		return AlcinaTemplateBeanProvider.get().getCommonPersistenceBean();
	}

	@Override
	public CommonPersistenceBase getCommonPersistenceExTransaction() {
		return new AlcinaTemplateCommonPersistence();
	}
}

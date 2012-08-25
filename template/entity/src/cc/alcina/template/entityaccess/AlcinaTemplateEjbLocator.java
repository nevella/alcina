package cc.alcina.template.entityaccess;


import cc.alcina.framework.entity.entityaccess.CommonPersistenceBase;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceLocal;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;

public class AlcinaTemplateEjbLocator implements CommonPersistenceProvider {
	private AlcinaTemplateEjbLocator() {
		super();
	}

	private static AlcinaTemplateEjbLocator theInstance;

	public static AlcinaTemplateEjbLocator get() {
		if (theInstance == null) {
			theInstance = new AlcinaTemplateEjbLocator();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}
	public CommonPersistenceLocal getCommonPersistence(){
		return AlcinaTemplateBeanProvider.get()
		.getCommonPersistenceBean();
	}

	@Override
	public CommonPersistenceBase getCommonPersistenceExTransaction() {
		return new AlcinaTemplateCommonPersistence();
	}
}

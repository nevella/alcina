package cc.alcina.framework.servlet.impl;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.entity.impl.PerDocumentSupplierCoreDocument;
import cc.alcina.framework.servlet.LifecycleService;

@Registration.Singleton(PerDocumentSupplierCoreDocumentRegistration.class)
public class PerDocumentSupplierCoreDocumentRegistration
		extends LifecycleService.AlsoDev {
	@Override
	public void onApplicationStartup() {
		// this class is only responsible for initialising the
		// PerDocumentSupplierCoreDocument class -
		// since that class is already a singleton (registered against
		// PerDocumentSupplier), it can't extend LifecycleService
		PerDocumentSupplierCoreDocument.cast().onAppStartup();
	}
}

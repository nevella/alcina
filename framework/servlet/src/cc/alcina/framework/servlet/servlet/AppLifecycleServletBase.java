package cc.alcina.framework.servlet.servlet;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.servlet.ServletLayerLocator;

public abstract class AppLifecycleServletBase extends GenericServlet{
	protected void createServletTransformClientInstance() {
		ThreadedPermissionsManager.cast().pushSystemUser();
		ClientInstance serverAsClientInstance = ServletLayerLocator.get()
				.commonPersistenceProvider().getCommonPersistence()
				.createClientInstance();
		CommonRemoteServiceServlet.serverAsClientInstance = serverAsClientInstance;
		PermissionsManager.get().popUser();
	}
}

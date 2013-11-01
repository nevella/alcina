package cc.alcina.template.servlet;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.servlet.CommonRemoteServletProvider;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;
@RegistryLocation(registryPoint = CommonRemoteServletProvider.class, implementationType = ImplementationType.SINGLETON)
public class AlcinaTemplateServerProvider implements
		CommonRemoteServletProvider {
	@Override
	public CommonRemoteServiceServlet getCommonRemoteServiceServlet() {
		return new AlcinaTemplateRemoteServiceImpl();
	}
}

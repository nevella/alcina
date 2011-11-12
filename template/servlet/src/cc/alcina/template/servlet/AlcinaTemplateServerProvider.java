package cc.alcina.template.servlet;

import cc.alcina.framework.servlet.CommonRemoteServletProvider;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

public class AlcinaTemplateServerProvider implements
		CommonRemoteServletProvider {
	@Override
	public CommonRemoteServiceServlet getCommonRemoteServiceServlet() {
		return new AlcinaTemplateRemoteServiceImpl();
	}
}

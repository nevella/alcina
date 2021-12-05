package cc.alcina.framework.servlet.servlet.ws;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

@Provider
public class AlcinaContextExitFilter implements ContainerResponseFilter {
	@Override
	public void filter(ContainerRequestContext arg0,
			ContainerResponseContext arg1) throws IOException {
		AlcinaServletContext.endContext();
	}
}

package cc.alcina.framework.servlet.servlet.ws;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

// Generic exception handler
// Avoids sending exceptions to the client
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
	private static final Logger LOGGER = 
			LoggerFactory.getLogger(GenericExceptionMapper.class);

	@Context 
	private HttpServletRequest request;

	@Override
	public Response toResponse(Exception e) {
		// End context in case it's there
		// AlcinaContextExitFilter should deal with this though
		AlcinaServletContext.endContext();
		String requestorIp = ServletLayerUtils.robustGetRemoteAddress(request);
		// TODO: Do something better than an if chain
		if (e instanceof NotFoundException) {
			LOGGER.warn("Unknown route {uri={}, ip={}}", 
					request.getRequestURI(), requestorIp);
			// Invalid request URI
			return Response.status(Status.NOT_FOUND).build();
		} else if (e instanceof NotAllowedException ||
				e instanceof NotSupportedException) {
			LOGGER.warn("Bad request type/body {uri={}, ip={}}", 
				request.getRequestURI(), requestorIp);
			// Invalid request type/body
			return Response.status(Status.BAD_REQUEST).build();
		} else {
			LOGGER.warn("Exception handling route {uri={}, ip={}}", 
				request.getRequestURI(), requestorIp);
			// Otherwise...
			e.printStackTrace();
			return Response.serverError().build();
		}
	}
}

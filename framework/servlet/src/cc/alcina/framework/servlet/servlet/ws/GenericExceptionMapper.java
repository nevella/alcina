package cc.alcina.framework.servlet.servlet.ws;

import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.servlet.AlcinaServletContext;
import cc.alcina.framework.servlet.servlet.HttpContext;

// Generic exception handler
// Avoids sending exceptions to the client
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(GenericExceptionMapper.class);

	@Override
	public Response toResponse(Exception e) {
		try {
			// will be null if failure occurs before entry filter (e.g. no path)
			HttpContext httpContext = AlcinaServletContext.httpContext();
			String requestorIp = httpContext == null ? "unknown"
					: ServletLayerUtils
							.robustGetRemoteAddress(httpContext.request);
			// TODO: Do something better than an if chain
			String requestURI = httpContext == null ? "unknown"
					: httpContext.request.getRequestURI();
			if (e instanceof NotFoundException) {
				LOGGER.warn("Unknown route {uri={}, ip={}}", requestURI,
						requestorIp);
				if (httpContext == null) {
					e.printStackTrace();
				}
				// Invalid request URI
				return Response.status(Status.NOT_FOUND).build();
			} else if (e instanceof NotAllowedException
					|| e instanceof NotSupportedException) {
				LOGGER.warn("Bad request type/body {uri={}, ip={}}", requestURI,
						requestorIp);
				e.printStackTrace();
				// Invalid request type/body
				return Response.status(Status.BAD_REQUEST).build();
			} else {
				LOGGER.warn("Exception handling route {uri={}, ip={}}",
						requestURI, requestorIp);
				// Otherwise...
				e.printStackTrace();
				return Response.serverError().build();
			}
		} finally {
			AlcinaServletContext.endContext();
		}
	}
}

package cc.alcina.framework.servlet.servlet.ws;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import cc.alcina.framework.servlet.servlet.AlcinaServletContext;

// Generic exception handler
// Avoids sending exceptions to the client
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
	@Override
	public Response toResponse(Exception e) {
		// End context in case it's there
		// AlcinaContextExitFilter should deal with this though
		AlcinaServletContext.endContext();
		// TODO: Do something better than an if chain
		if (e instanceof NotFoundException) {
			// Invalid request URI
			return Response.status(Status.NOT_FOUND).build();
		} else {
			// Otherwise...
			e.printStackTrace();
			return Response.serverError().build();
		}
	}
}

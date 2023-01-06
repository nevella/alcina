package cc.alcina.framework.servlet.servlet.ws;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Extend this class to support all options (for a given resource provider with
 * an @Path annotation)
 *
 * @author nick@alcina.cc
 *
 */
public class AllOptions {
	@OPTIONS
	@Path("{path: .*}")
	public Response allOptions() {
		return Response.ok().build();
	}
}

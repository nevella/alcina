package cc.alcina.framework.servlet.servlet.ws;

import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Consumes(MediaType.MEDIA_TYPE_WILDCARD)
public class AllOptions {
	@OPTIONS
	@Path("{path: .*}")
	public Response allOptions() {
		return Response.ok().build();
	}
}

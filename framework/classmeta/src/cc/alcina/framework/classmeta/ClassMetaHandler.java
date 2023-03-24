package cc.alcina.framework.classmeta;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.util.JacksonUtils;

public class ClassMetaHandler extends AbstractHandler {
	ClasspathScannerResolver classpathScannerResolver = new ClasspathScannerResolver();

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			MetricLogging.get().start("class-meta");
			ServletInputStream inputStream = request.getInputStream();
			String json = Io.read().fromStream(inputStream).asString();
			ClassMetaRequest typedRequest = JacksonUtils.deserialize(json,
					ClassMetaRequest.class);
			ClassMetaResponse typedResponse = new ClassMetaResponse();
			typedResponse.request = typedRequest;
			switch (typedRequest.type) {
			case Classes:
				boolean debug = false;
				String translationKey = request.getParameter("translation");
				typedResponse.cache = classpathScannerResolver
						.handle(typedRequest, translationKey, debug);
				if (debug) {
					Io.log().toFile(typedResponse.cache.dump());
				}
				break;
			default:
				throw new UnsupportedOperationException();
			}
			String resultJson = JacksonUtils.serialize(typedResponse);
			response.getWriter().write(resultJson);
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			Ax.out("Served class meta: %s", typedResponse);
			MetricLogging.get().end("class-meta");
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	public void refreshJars() {
		classpathScannerResolver.refreshJars();
	}
}

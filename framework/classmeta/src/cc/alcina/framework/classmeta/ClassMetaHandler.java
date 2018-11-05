package cc.alcina.framework.classmeta;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public class ClassMetaHandler extends AbstractHandler {
	ClasspathScannerResolver classpathScannerResolver = new ClasspathScannerResolver();

	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			LooseContext.pushWithTrue(
					KryoUtils.CONTEXT_USE_UNSAFE_FIELD_SERIALIZER);
			ServletInputStream inputStream = request.getInputStream();
			byte[] byteArray = ResourceUtilities
					.readStreamToByteArray(inputStream);
			String payload = new String(byteArray, "UTF-8");
			ClassMetaRequest typedRequest = KryoUtils
					.deserializeFromBase64(payload, ClassMetaRequest.class);
			ClassMetaResponse typedResponse = new ClassMetaResponse();
			typedResponse.request = typedRequest;
			switch (typedRequest.type) {
			case Classes:
				boolean debug = false;
				typedResponse.cache = classpathScannerResolver
						.handle(typedRequest, debug);
				if (debug) {
					ResourceUtilities.logToFile(typedResponse.cache.dump());
				}
				break;
			default:
				throw new UnsupportedOperationException();
			}
			KryoUtils.serializeToStream(typedResponse,
					response.getOutputStream());
			response.setContentType("application/octet-stream");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			Ax.out("Served class meta: %s", typedResponse);
		} finally {
			LooseContext.pop();
		}
	}

	public void refreshJars() {
		classpathScannerResolver.refreshJars();
	}
}

package cc.alcina.framework.servlet.component.test.server;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;

/*
 * Provides a server for test gwt.xml content
 */
public class AlcinaDevTestHandler {
	public AlcinaDevTestHandler() {
		URL url = getResourceUrl("/test.html");
		if (url == null) {
			throw new RuntimeException("Unable to find resource directory");
		}
	}

	URL getResourceUrl(String warRelativePart) {
		ClassLoader cl = AlcinaDevTestHandler.class.getClassLoader();
		return cl.getResource(
				Ax.format("cc/alcina/framework/servlet/component/test/war%s",
						warRelativePart));
	}

	public void handle(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String method = request.getMethod();
		switch (method) {
		case "GET":
			serveFile(request, response);
			break;
		case "POST":
			throw new UnsupportedOperationException();
		default:
			throw new UnsupportedOperationException();
		}
	}

	void serveFile(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		String path = request.getPathInfo();
		if (path == null) {
			path = "/test.html";
		}
		boolean containerHtml = path.equals("/test.html");
		URL url = getResourceUrl(path);
		if (url == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		boolean isSecureLocalhost = Ax.matches(request.getHeader("host"),
				"127.0.0.1(:\\d+)?");
		if (request.isSecure() || isSecureLocalhost) {
			// persuade the browser (at least Chrome) to allow GWT dev
			// mode/ws
			// (sharedarraybuffer)
			response.addHeader("Cross-Origin-Opener-Policy", "same-origin");
			response.addHeader("Cross-Origin-Embedder-Policy", "require-corp");
		}
		String suffix = path.replaceFirst(".+\\.(.+)", "$1");
		switch (suffix) {
		case "html":
			response.setContentType("text/html");
			break;
		case "json":
			response.setContentType("application/json");
			break;
		case "js":
			response.setContentType("text/javascript");
			break;
		case "gif":
			response.setContentType("image/gif");
			break;
		default:
			throw new UnsupportedOperationException();
		}
		if (containerHtml) {
			String bootstrapHtml = Io.read().fromStream(url.openStream())
					.asString();
			URL nocacheJsUrl = getResourceUrl(
					"/cc.alcina.framework.servlet.component.test.AlcinaGwtTestClient/cc.alcina.framework.servlet.component.test.AlcinaGwtTestClient.nocache.js");
			String nocacheJs = Io.read().fromStream(nocacheJsUrl.openStream())
					.asString();
			bootstrapHtml = bootstrapHtml.replace("<!--%%NOCACHE_JS%%-->",
					nocacheJs);
			Io.write().string(bootstrapHtml)
					.toStream(response.getOutputStream());
		} else {
			Io.read().fromStream(url.openStream()).write()
					.toStream(response.getOutputStream());
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	Logger logger = LoggerFactory.getLogger(getClass());
}

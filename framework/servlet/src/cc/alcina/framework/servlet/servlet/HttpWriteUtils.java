package cc.alcina.framework.servlet.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public interface HttpWriteUtils {
	default void writeAndClose(String s, HttpServletResponse response)
			throws IOException {
		response.setContentType("text/plain");
		response.getWriter().write(s);
		response.getWriter().close();
	}

	default void writeHtmlResponse(HttpServletResponse response, String string)
			throws IOException {
		if (response == null) {
			System.out.println(CommonUtils.trimToWsChars(string, 1000));
		} else {
			response.setContentType("text/html");
			response.getWriter().write(string);
		}
	}

	default void writeTextResponse(HttpServletResponse response, String string)
			throws IOException {
		if (response == null) {
			System.out.println(CommonUtils.trimToWsChars(string, 1000));
		} else {
			try {
				response.setContentType("text/plain");
				response.getWriter().write(string);
			} catch (Exception e) {
				Ax.out("Writing text response: ", string);
				e.printStackTrace();
			}
		}
	}
}

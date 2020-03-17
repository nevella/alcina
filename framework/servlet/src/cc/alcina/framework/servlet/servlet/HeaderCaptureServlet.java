/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.servlet.servlet;

import java.util.ArrayList;
import java.util.Collections;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.servlet.ServletLayerUtils;

/**
 *
 * @author Nick Reddel
 */
public class HeaderCaptureServlet extends HttpServlet {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) {
		ArrayList<String> names = Collections.list(request.getHeaderNames());
		FormatBuilder logBuilder = new FormatBuilder().separator(" :--: ");
		FormatBuilder outputBuilder = new FormatBuilder().separator("\n");
		String ipAddr = ServletLayerUtils.robustGetRemoteAddr(request);
		outputBuilder.format(
				"Header capture - IP address %s\n=================================================\n",
				ipAddr);
		logBuilder.format("Header capture - IP address %s",
				ipAddr);
		for (String name : names) {
			ArrayList<String> values = Collections
					.list(request.getHeaders(name));
			String log = Ax.format("%s:%s", name,
					values.size() == 1 ? values.get(0) : values);
			logBuilder.append(log);
			outputBuilder.append(log);
		}
		logger.info(logBuilder.toString());
		response.setContentType("text/plain");
		try {
			response.getWriter().write(outputBuilder.toString());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}

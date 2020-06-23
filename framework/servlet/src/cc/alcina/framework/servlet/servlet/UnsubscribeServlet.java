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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.projection.GraphProjection;

/**
 *
 * @author Nick Reddel
 */
public class UnsubscribeServlet extends AlcinaServlet {
	private static final String DEFAULT_SERVLET_PATH = "unsubscribe.do";

	public static String defaultHref(PublicationResult publicationResult) {
		UnsubscribeRequest request = new UnsubscribeRequest();
		request.publicationId = publicationResult.publicationId;
		request.publicationUid = publicationResult.publicationUid;
		request.resubscribe = false;
		return Ax.format("%s?%s", DEFAULT_SERVLET_PATH, request.serialize());
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		FormatBuilder outputBuilder = new FormatBuilder();
		String queryString = request.getQueryString();
		try {
			String[] parts = queryString.split("/");
			UnsubscribeRequest unsubscribe = new UnsubscribeRequest();
			unsubscribe.publicationId = Long.parseLong(parts[0]);
			unsubscribe.publicationUid = parts[1];
			unsubscribe.resubscribe = parts[2].equals("r");
			logger.info(GraphProjection.fieldwiseToString(unsubscribe));
			String message = Registry.impl(UnsubscribeHandler.class)
					.handle(unsubscribe);
			outputBuilder.line(message);
		} catch (Exception e) {
			e.printStackTrace();
			outputBuilder.line("Unable to process unsubscribe request: %s",
					queryString);
		}
		response.setContentType("text/plain");
		try {
			response.getWriter().write(outputBuilder.toString());
			logger.info(outputBuilder.toString());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@RegistryLocation(registryPoint = UnsubscribeHandler.class, implementationType = ImplementationType.INSTANCE)
	public abstract static class UnsubscribeHandler {
		protected abstract String handle(UnsubscribeRequest unsubscribe);
	}

	public static class UnsubscribeRequest {
		public long publicationId;

		public String publicationUid;

		public boolean resubscribe;

		public String serialize() {
			return new FormatBuilder().separator("/").append(publicationId)
					.append(publicationUid).append(resubscribe ? "r" : "u")
					.toString();
		}
	}
}

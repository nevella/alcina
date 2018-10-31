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

import java.util.Objects;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.KnownsDelta;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.knowns.Knowns;
import cc.alcina.framework.servlet.knowns.KnownsDeltaRequestHandler;

/**
 *
 * @author Nick Reddel
 */
public class KnownsServlet extends HttpServlet {
	@Override
	public void destroy() {
		Knowns.shutdown();
		super.destroy();
	}

	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) {
		checkAuthenticated(request, response);
		String clientId = request.getParameter("clientId");
		String s_since = request.getParameter("since");
		long since = Long.parseLong(s_since);
		KnownsDelta delta = new KnownsDeltaRequestHandler().getDelta(since,
				clientId);
		String base64 = KryoUtils.serializeToBase64(delta);
		response.setContentType("text/plain");
		try {
			response.getWriter().write(base64);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void checkAuthenticated(HttpServletRequest request,
			HttpServletResponse response) {
		String auth = ResourceUtilities.get(KnownsServlet.class, "auth");
		if (auth.isEmpty()
				|| !Objects.equals(request.getParameter("auth"), auth)) {
			throw new RuntimeException("Access not permitted");
		}
	}
}

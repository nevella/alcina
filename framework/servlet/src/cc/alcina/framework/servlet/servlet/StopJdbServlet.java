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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.servlet.actionhandlers.jdb.RemoteDebugHandler;

/**
 *
 * @author Nick Reddel
 */
public class StopJdbServlet extends AlcinaServlet {



	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try  {
			String message = RemoteDebugHandler.JdbWrapper.get().stopJdb();
			writeTextResponse(response, message);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}

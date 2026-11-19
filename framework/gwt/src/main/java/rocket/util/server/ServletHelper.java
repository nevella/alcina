/*
 * Copyright Miroslav Pokorny
 *
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
package rocket.util.server;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import rocket.util.client.Checker;

/**
 * A collection of helpful servlet methods
 * 
 * @author Miroslav Pokorny (mP)
 */
public class ServletHelper {
	public static void writeBytes(final String mimeType, final byte[] bytes,
			final HttpServletResponse response) throws IOException {
		Checker.notEmpty("parameter:mimeType", mimeType);
		Checker.notNull("parameter:bytes", bytes);
		Checker.notNull("parameter:response", response);
		OutputStream out = null;
		try {
			response.setContentType(mimeType);
			response.setContentLength(bytes.length);
			out = response.getOutputStream();
			out.write(bytes);
			out.flush();
		} finally {
			InputOutput.closeIfNecessary(out);
		}
	}
}
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

package cc.alcina.framework.gwt.client.logic;

import java.util.Date;

import cc.alcina.framework.common.client.util.CurrentUtcDateProvider;


/**
 *
 * @author Nick Reddel
 */

 public class ClientUtils implements CurrentUtcDateProvider {
	private ClientUtils() {
		super();
	}

	private static ClientUtils theInstance;

	public static ClientUtils get() {
		if (theInstance == null) {
			theInstance = new ClientUtils();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	@SuppressWarnings("deprecation")
	public Date currentUtcDate() {
		Date d = new Date();
		return new Date(d.getTime() + d.getTimezoneOffset() * 60 * 1000);
	}
}

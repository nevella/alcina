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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.History;

/**
 * 
 * @author Nick Reddel
 */
public class AlcinaDebugIds {
	public static final String LOGIN_FORM = "login-form";

	public static final String LOGIN_USERNAME = "login-username";

	public static final String LOGIN_PASSWORD = "login-password";

	public static final String LOGIN_SUBMIT = "login-submit";

	public static final String TOP_BUTTON = "top-button";

	public static final String TOP_BUTTON_LOGIN = "login";

	public static final String TOP_BUTTON_LOGOUT = "logout";

	public static final String TOP_BUTTON_LOGIN_STATUS = "login-status";

	public static final String TOP_BUTTON_OPTIONS = "options";

	public static final String MISC_ALCINA_BEAN_PANEL = "alcina-BeanPanel";

	public static final String ACTION_VIEW_RUN = "action-view-run";

	public static final String GRID_FORM_FIELD_DEBUG_PREFIX = "GridForm-";

	public static final String DEBUG_SIMULATE_OFFLINE = "d-simulate-offline";
	public static final String DEBUG_LOG_LOAD_METRICS = "d-load-metrics";

	public static final List<String> DEBUG_IDS = new ArrayList(Arrays
			.asList(new String[] { DEBUG_SIMULATE_OFFLINE ,DEBUG_LOG_LOAD_METRICS}));

	private static List<String> debugIdsMatched = new ArrayList<String>();

	public static String getButtonId(String key) {
		return TOP_BUTTON + "-" + key;
	}

	public static boolean hasFlag(String key) {
		return debugIdsMatched.contains(key);
	}
	
	public static void setFlag(String key) {
		debugIdsMatched.add(key);
	}

	public static void initialise() {
		String token = History.getToken();
		if (token != null) {
			AlcinaHistoryItem currentEvent = AlcinaHistory.get()
					.parseToken(token);
			if (currentEvent != null) {
				for (String dbgId : DEBUG_IDS) {
					if (currentEvent.hasParameter(dbgId)) {
						debugIdsMatched.add(dbgId);
					}
				}
			}
		}
	}
}

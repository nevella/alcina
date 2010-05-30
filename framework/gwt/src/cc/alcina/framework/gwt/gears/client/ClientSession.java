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
package cc.alcina.framework.gwt.gears.client;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;

/**
 * 
 * @author nick@alcina.cc
 *         <p>
 *         Basic session support for the (possibly offline) client
 *         </p>
 * 
 *         Logic:
 *         <ul>
 *         <li>Each client (browser "process", pace chrome) has a storage
 *         session</li>
 *         <li>For simplicity, only one offline instance (tab) can be run per
 *         browser instance (one per clientInstanceId would be possible, but
 *         probably confusin to users)</li>
 *         <li>Also, the save_initial function is only called once (first time)
 *         per session - saves on wear'n'tear</li>
 *         <li>Tabs communicate via cookies n timers</li>
 *         </ul>
 * 
 */
public class ClientSession {
	public static final int KEEP_ALIVE_TIMER = 2000;

	private static final String STORAGE_SESSION_COOKIE_NAME = ClientSession.class
			.getName()
			+ ".storage-session";

	private static final String HAS_PERSISTED_INITIAL_OBJECTS_COOKIE_NAME = ClientSession.class
			.getName()
			+ ".initial-objects-persisted";

	private ClientSession() {
		super();
		Map<Long, Long> m = parseCookie();
		Long maxTabId=m.isEmpty()?0:CommonUtils.last(m.keySet().iterator());
		tabId=maxTabId+1;
		updateCookie();
		new Timer() {
			@Override
			public void run() {
				updateCookie();
			}
		}.scheduleRepeating(KEEP_ALIVE_TIMER);
	}
	private static ClientSession theInstance;

	private long tabId;

	public static ClientSession get() {
		if (theInstance == null) {
			theInstance = new ClientSession();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}


	protected void updateCookie() {
		Map<Long, Long> m = parseCookie();
		m.put(tabId, System.currentTimeMillis());
		StringBuilder sb = new StringBuilder();
		for (Long k : m.keySet()) {
			sb.append(k);
			sb.append(",");
			sb.append(m.get(k));
			sb.append(",");
		}
		Cookies.setCookie(STORAGE_SESSION_COOKIE_NAME, sb.toString());
	}

	protected Map<Long, Long> parseCookie() {
		Map<Long, Long> result = new LinkedHashMap<Long, Long>();
		String s = Cookies.getCookie(STORAGE_SESSION_COOKIE_NAME);
		if (s != null) {
			String[] split = s.split(",");
			for (int i = 0; i < split.length; i += 2) {
				result.put(Long.parseLong(split[i]), Long
						.parseLong(split[i + 1]));
			}
		}
		return result;
	}

	public boolean isSoleOpenTab() {
		long l = System.currentTimeMillis();
		Map<Long, Long> m = parseCookie();
		for (Long k : m.keySet()) {
			if (k != tabId && (l - m.get(k)) < 3000) {
				return false;
			}
		}
		return true;
	}

	public void setInitialObjectsPersisted(boolean initialObjectsPersisted) {
		Cookies.setCookie(HAS_PERSISTED_INITIAL_OBJECTS_COOKIE_NAME,String.valueOf(initialObjectsPersisted));
	}

	public boolean isInitialObjectsPersisted() {
		String s=Cookies.getCookie(HAS_PERSISTED_INITIAL_OBJECTS_COOKIE_NAME);
		return s!=null && Boolean.valueOf(s);
	}
	
}

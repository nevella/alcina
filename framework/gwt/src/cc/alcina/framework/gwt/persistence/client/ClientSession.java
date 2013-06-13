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
package cc.alcina.framework.gwt.persistence.client;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

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
public class ClientSession implements ClosingHandler {
	public static final int KEEP_ALIVE_TIMER = 2000;

	public static void registerImplementation(ClientSession impl) {
		theInstance = impl;
	}

	private String storageSessionCookieName;
	
	private String hasPersistedInitialObjectsCookieName;

	private String persistingChunkCookieName;

	private static ClientSession theInstance;

	public static ClientSession get() {
		if (theInstance == null) {
			theInstance = new ClientSession();
		}
		return theInstance;
	}

	private long tabId;

	private Timer updateTimer;

	private ClientSession() {
		super();
		setAppId("alcina");
		reset();
		Window.addWindowClosingHandler((ClosingHandler) this);
	}

	public void appShutdown() {
		updateCookie(true);
		theInstance = null;
	}

	public void cancelSession() {
		if (updateTimer != null) {
			updateTimer.cancel();
		}
		updateCookie(true);
	}

	/**
	 * Callback with true if sole open tab
	 */
	public void checkSoleOpenTab(final Callback<Boolean> callback) {
		if (isSoleOpenTab()) {
			callback.apply(true);
			return;
		}
		new Timer() {
			int retryCount = 4;

			@Override
			public void run() {
				if (retryCount-- == 0) {
					cancel();
					callback.apply(false);
				} else if (isSoleOpenTab()) {
					cancel();
					callback.apply(true);
				}
			}
		}.scheduleRepeating(1000);
	}

	public boolean isInitialObjectsPersisted() {
		String s = Cookies.getCookie(hasPersistedInitialObjectsCookieName);
		return s != null && Boolean.valueOf(s);
	}
	
	public boolean isPersistingChunk() {
		String s = Cookies.getCookie(this.persistingChunkCookieName);
		return s != null && Boolean.valueOf(s);
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

	@Override
	public void onWindowClosing(ClosingEvent event) {
		ClientSession.get().appShutdown();
	}

	public void setAppId(String appId) {
		hasPersistedInitialObjectsCookieName = CommonUtils.formatJ("%s.%s.%s",
				ClientSession.class.getName(), appId,
				"initial-objects-persisted");
		storageSessionCookieName = CommonUtils.formatJ("%s.%s.%s",
				ClientSession.class.getName(), appId, "storage-session");
		persistingChunkCookieName = CommonUtils.formatJ("%s.%s.%s",
				ClientSession.class.getName(), appId, "persisting-chunk");
		reset();
	}

	public void setInitialObjectsPersisted(boolean initialObjectsPersisted) {
		Cookies.setCookie(hasPersistedInitialObjectsCookieName,
				String.valueOf(initialObjectsPersisted));
	}
	
	public void setPersistingChunk(boolean persistingChunk) {
		Cookies.setCookie(persistingChunkCookieName,
				String.valueOf(persistingChunk));
	}

	protected Map<Long, Long> parseCookie() {
		Map<Long, Long> result = new LinkedHashMap<Long, Long>();
		String s = Cookies.getCookie(storageSessionCookieName);
		try {
			if (s != null && !s.isEmpty()) {
				String[] split = s.split(",");
				for (int i = 0; i < split.length; i += 2) {
					result.put(Long.parseLong(split[i]),
							Long.parseLong(split[i + 1]));
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
			Cookies.removeCookie(storageSessionCookieName);
		}
		return result;
	}

	protected void reset() {
		if (updateTimer != null) {
			updateTimer.cancel();
		}
		Map<Long, Long> m = parseCookie();
		Long maxTabId = m.isEmpty() ? 0 : CommonUtils.last(m.keySet()
				.iterator());
		tabId = maxTabId + 1;
		updateCookie(false);
		updateTimer = new Timer() {
			@Override
			public void run() {
				updateCookie(false);
			}
		};
		updateTimer.scheduleRepeating(KEEP_ALIVE_TIMER);
	}

	protected void updateCookie(boolean remove) {
		Map<Long, Long> m = parseCookie();
		if (remove) {
			m.remove(tabId);
		} else {
			m.put(tabId, System.currentTimeMillis());
		}
		StringBuilder sb = new StringBuilder();
		for (Long k : m.keySet()) {
			sb.append(k);
			sb.append(",");
			sb.append(m.get(k));
			sb.append(",");
		}
		Cookies.setCookie(storageSessionCookieName, sb.toString());
	}

	public static class ClientSessionSingleAccess extends ClientSession {
		private boolean initialObjectsPersisted;

		public boolean isInitialObjectsPersisted() {
			return this.initialObjectsPersisted;
		}

		@Override
		public boolean isSoleOpenTab() {
			return true;
		}

		public void setInitialObjectsPersisted(boolean initialObjectsPersisted) {
			this.initialObjectsPersisted = initialObjectsPersisted;
		}

		@Override
		protected void reset() {
		}
	}
}

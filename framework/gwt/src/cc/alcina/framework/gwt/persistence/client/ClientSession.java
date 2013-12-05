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
import java.util.Map.Entry;

import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

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
public class ClientSession implements ClosingHandler, RegistrableService {
	public static final int KEEP_ALIVE_TIMER = 1000;

	public static final long EXPIRES_TIME = 2500;

	static class CrossTabCookie {
		private String cookieName;

		public CrossTabCookie(String cookieName) {
			this.cookieName = cookieName;
		}

		boolean active;

		private Long tabId;

		public boolean isActive() {
			return parseCookie().containsKey(tabId);
		}

		public boolean isSoleTab() {
			Map<Long, Long> idTimeMap = parseCookie();
			removeExpiredTabs(idTimeMap);
			idTimeMap = parseCookie();
			if (idTimeMap.size() == 0
					|| (tabId != null && idTimeMap.size() == 1 && idTimeMap
							.keySet().iterator().next() == tabId)) {
				return true;
			} else {
				return false;
			}
		}

		private Timer refreshTimer;

		public void setActive(boolean active) {
			if (active) {
				if (refreshTimer == null) {
					refreshTimer = new Timer() {
						@Override
						public void run() {
							setActive(true);
						}
					};
					refreshTimer.scheduleRepeating(KEEP_ALIVE_TIMER);
				}
			} else {
				if (refreshTimer != null) {
					refreshTimer.cancel();
					refreshTimer = null;
				}
			}
			if (tabId == null) {
				if (active) {
					Map<Long, Long> m = parseCookie();
					Long maxTabId = m.isEmpty() ? 0 : CommonUtils.last(m
							.keySet().iterator());
					tabId = maxTabId + 1;
				} else {
					return;
				}
			}
			Map<Long, Long> m = parseCookie();
			if (active) {
				if (!m.containsKey(tabId)) {
				}
				m.put(tabId, System.currentTimeMillis());
			} else {
				m.remove(tabId);
				tabId = null;
			}
			removeExpiredTabs(m);
		}

		protected void removeExpiredTabs(Map<Long, Long> m) {
			StringBuilder sb = new StringBuilder();
			for (Entry<Long, Long> entry : m.entrySet()) {
				long ckTabId = entry.getKey();
				long ckUpdateTime = entry.getValue();
				if (ckUpdateTime >= (System.currentTimeMillis() - EXPIRES_TIME)) {
					sb.append(ckTabId);
					sb.append(",");
					sb.append(ckUpdateTime);
					sb.append(",");
				} else {
				}
			}
			Cookies.setCookie(cookieName, sb.toString());
		}

		protected Map<Long, Long> parseCookie() {
			Map<Long, Long> result = new LinkedHashMap<Long, Long>();
			String s = Cookies.getCookie(cookieName);
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
				Cookies.removeCookie(cookieName);
			}
			return result;
		}
	}

	private String storageSessionCookieName;

	private String hasPersistedInitialObjectsCookieName;

	private String persistingChunkCookieName;

	public static ClientSession get() {
		ClientSession singleton = Registry.checkSingleton(ClientSession.class);
		if (singleton == null) {
			singleton = new ClientSession();
			Registry.registerSingleton(ClientSession.class, singleton);
		}
		return singleton;
	}

	private CrossTabCookie storageCookie;

	private CrossTabCookie persistingChunkCookie;

	private ClientSession() {
		super();
		init();
	}

	protected void init() {
		Window.addWindowClosingHandler((ClosingHandler) this);
		setAppId("alcina");
	}

	private void createCookies() {
		storageCookie = new CrossTabCookie(storageSessionCookieName);
		storageCookie.setActive(true);
		persistingChunkCookie = new CrossTabCookie(persistingChunkCookieName);
	}

	private void deactivateCookies() {
		if (storageCookie != null) {
			storageCookie.setActive(false);
			persistingChunkCookie.setActive(false);
		}
	}

	public void resetCookies() {
		deactivateCookies();
		createCookies();
	}

	public void appShutdown() {
		deactivateCookies();
	}

	public void cancelSession() {
		deactivateCookies();
	}

	/**
	 * Callback with true if sole open tab
	 */
	public void checkSoleOpenTab(final AsyncCallback<Boolean> callback) {
		if (isSoleOpenTab()) {
			callback.onSuccess(true);
			return;
		}
		new Timer() {
			int retryCount = 4;

			@Override
			public void run() {
				if (retryCount-- == 0) {
					cancel();
					callback.onSuccess(false);
				} else if (isSoleOpenTab()) {
					cancel();
					callback.onSuccess(true);
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
		return storageCookie.isSoleTab();
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
		resetCookies();
	}

	public void setInitialObjectsPersisted(boolean initialObjectsPersisted) {
		Cookies.setCookie(hasPersistedInitialObjectsCookieName,
				String.valueOf(initialObjectsPersisted));
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
		public void resetCookies() {
		}
		@Override
		public void releaseCrossTabPersistenceLock() {
		}

		@Override
		public void acquireCrossTabPersistenceLock(AsyncCallback<Void> callback) {
			callback.onSuccess(null);
		}
	}

	public void acquireCrossTabPersistenceLock(
			final AsyncCallback<Void> callback) {
		if (persistingChunkCookie.isSoleTab()) {
			persistingChunkCookie.setActive(true);
			callback.onSuccess(null);
			return;
		}
		new Timer() {
			@Override
			public void run() {
				acquireCrossTabPersistenceLock(callback);
			}
		}.schedule(1000);
	}

	public void releaseCrossTabPersistenceLock() {
		persistingChunkCookie.setActive(false);
	}
}

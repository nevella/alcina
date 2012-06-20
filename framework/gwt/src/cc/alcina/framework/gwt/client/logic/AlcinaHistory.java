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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.URLComponentEncoder;

import com.google.gwt.user.client.History;

/**
 * 
 * @author Nick Reddel
 */
public class AlcinaHistory<I extends AlcinaHistoryItem> {
	public static final String LOGIN_EVENT = "LOGIN_EVENT";

	public static final String SEARCH = "SEARCH";

	public static final String LOCATION_KEY = "t";

	public static final String CONTENT_KEY = "c";

	public static final String SEARCH_INDEX = "sdi";

	public static final String SEARCH_PAGE = "sdp";

	public static final String NO_HISTORY_KEY = "nh";

	public static final String ACTION_KEY = "a";

	public static final String ID_KEY = "id";

	public static final String LOCAL_ID_KEY = "lid";

	public static final String CLASS_NAME_KEY = "cn";

	public static final String Y_KEY = "y";

	private static AlcinaHistory theInstance;

	public static AlcinaHistory get() {
		if (theInstance == null) {
			theInstance = new AlcinaHistory();
		}
		return theInstance;
	}

	public static void register(AlcinaHistory subclass) {
		theInstance = subclass;
	}

	private boolean noHistoryDisabled = false;

	protected I currentEvent;

	protected I lastEvent;

	protected String lastHistoryToken;

	protected Map<String, String> tokenDisplayNames = new HashMap<String, String>();

	protected AlcinaHistory() {
		super();
	}

	public void appShutdown() {
		theInstance = null;
	}

	public I createHistoryInfo() {
		return (I) new AlcinaHistoryItem();
	}

	public String getContentLink(String contentKey) {
		I hib = createHistoryInfo();
		hib.setContentToken(contentKey);
		return hib.toTokenString();
	}

	public I getCurrentEvent() {
		return this.currentEvent;
	}

	public I getLastEvent() {
		return this.lastEvent;
	}

	public String getLastHistoryToken() {
		return lastHistoryToken;
	}

	public String getTabLink(String tabId) {
		AlcinaHistoryItem hib = createHistoryInfo();
		hib.setTabName(tabId);
		return hib.toTokenString();
	}

	public String getTabSubkeyLink(String tabId, String subkeyId) {
		AlcinaHistoryItem hib = createHistoryInfo();
		hib.setTabName(tabId);
		hib.setSubTabName(subkeyId);
		return hib.toTokenString();
	}

	public String getTokenDisplayName(String token) {
		initTokenDisplayNames();
		return tokenDisplayNames.get(token);
	}

	public boolean isEquivalentToCurrent(I info, String... keys) {
		if (currentEvent == null) {
			return info == null;
		}
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			if (key.equals(LOCATION_KEY)) {
				int locationIndex = Integer.parseInt(keys[++i]);
				if (!CommonUtils.equalsWithNullEquality(
						currentEvent.getLocationPart(locationIndex),
						info.getLocationPart(locationIndex))) {
					return false;
				}
			} else {
				if (!CommonUtils.equalsWithNullEquality(
						currentEvent.getStringParameter(key),
						info.getStringParameter(key))) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isNoHistoryDisabled() {
		return this.noHistoryDisabled;
	}

	public void onHistoryChanged(String historyToken) {
		lastEvent = currentEvent;
		currentEvent = parseToken(historyToken);
	}

	public I ensureEventFromCurrentToken() {
		onHistoryChanged(History.getToken());
		return getCurrentEvent();
	}

	public I parseToken(String historyToken) {
		I item = createHistoryInfo();
		Map<String, String> params = item.parseParameters(historyToken);
		if (params.size() == 0) {
			item.notAHistoryToken = true;
			return item;
		} else {
			lastHistoryToken = historyToken;
		}
		if (item.getTabName() != null) {
			item.type = HistoryEventType.TABBED;
		}
		return item;
	}

	public void setLastHistoryToken(String lastHistoryToken) {
		this.lastHistoryToken = lastHistoryToken;
	}

	public void setNoHistoryDisabled(boolean noHistoryDisabled) {
		this.noHistoryDisabled = noHistoryDisabled;
	}

	public static String toHash(Map<String, String> params) {
		StringBuffer sb = new StringBuffer();
		ArrayList<String> keys = new ArrayList<String>(params.keySet());
		URLComponentEncoder encoder = CommonLocator.get().urlComponentEncoder();
		Collections.sort(keys);
		for (String k : keys) {
			if (params.get(k) == null) {
				continue;
			}
			if (sb.length() != 0) {
				sb.append("&");
			}
			sb.append(k);
			sb.append("=");
			sb.append(encoder.encode(params.get(k).toString()));
		}
		return sb.toString();
	}

	public String tokenForSearch(SearchDefinition def, int pageNumber) {
		AlcinaHistoryItem hib = createHistoryInfo();
		hib.setSearchHistoryInfo(new SearchHistoryInfo(def
				.getClientSearchIndex(), pageNumber));
		return hib.toTokenString();
	}

	protected void initTokenDisplayNames() {
	}

	public enum HistoryEventType {
		NO_TAB_SPEC, UNTABBED, TABBED
	}

	public static class SearchHistoryInfo {
		public int defId;

		public int pageNumber;

		public SearchHistoryInfo(int defId, int pageNumber) {
			this.defId = defId;
			this.pageNumber = pageNumber;
		}
	}

	public static class SimpleHistoryEventInfo {
		public String historyToken;

		public String displayName;

		public SimpleHistoryEventInfo() {
		}

		public SimpleHistoryEventInfo(String displayName) {
			this(displayName, "");
		}

		public SimpleHistoryEventInfo(String displayName, String historyToken) {
			this.displayName = displayName;
			this.historyToken = historyToken;
		}
	}
}

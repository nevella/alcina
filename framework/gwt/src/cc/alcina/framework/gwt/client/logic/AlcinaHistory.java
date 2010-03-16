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
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.http.client.URL;

/**
 *
 * @author Nick Reddel
 */

 public class AlcinaHistory {
	public static final String LOGIN_EVENT = "LOGIN_EVENT";

	public static final String SEARCH = "SEARCH";

	public static final String TAB_KEY = "t";

	public static final String CONTENT_KEY = "c";

	public static final String TAB_SUB_KEY = "ts";

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

	private boolean noHistoryDisabled = false;

	protected HistoryInfoBase currentEvent;

	protected HistoryInfoBase lastEvent;

	protected String lastHistoryToken;

	protected Map<String, String> tokenDisplayNames = new HashMap<String, String>();

	protected AlcinaHistory() {
		super();
	}

	public void appShutdown() {
		theInstance = null;
	}

	public HistoryInfoBase createHistoryInfo() {
		return new HistoryInfoBase();
	}

	public String getContentLink(String tabId) {
		return CONTENT_KEY + "=" + tabId;
	}

	public HistoryInfoBase getCurrentEvent() {
		return this.currentEvent;
	}

	public HistoryInfoBase getLastEvent() {
		return this.lastEvent;
	}

	public String getLastHistoryToken() {
		return lastHistoryToken;
	}

	public String getTabLink(String tabId) {
		return TAB_KEY + "=" + tabId;
	}

	public String getTabSubkeyLink(String tabId, String subkeyId) {
		return TAB_KEY + "=" + tabId + "&" + TAB_SUB_KEY + "=" + subkeyId;
	}

	public String getTokenDisplayName(String token) {
		initTokenDisplayNames();
		return tokenDisplayNames.get(token);
	}

	public boolean isCurrentTabAndSubtab(HistoryInfoBase info) {
		if (currentEvent == null) {
			return info == null;
		}
		return CommonUtils.equalsWithNullEquality(currentEvent.tabName,
				info.tabName)
				&& CommonUtils.equalsWithNullEquality(currentEvent.subTabName,
						info.subTabName);
	}

	public boolean isNoHistoryDisabled() {
		return this.noHistoryDisabled;
	}

	public void onHistoryChanged(String historyToken) {
		lastEvent = currentEvent;
		currentEvent = parseToken(historyToken);
	}

	public Map<String, String> parseParameters(String s) {
		Map<String, String> params = new HashMap<String, String>();
		String[] pairs = s.split("&");
		for (String pair : pairs) {
			String[] split = pair.split("=");
			if (split.length == 2) {
				params.put(split[0], URL.decodeComponent(split[1]));
			}
		}
		return params;
	}

	public HistoryInfoBase parseToken(String historyToken) {
		HistoryInfoBase info = createHistoryInfo();
		Map<String, String> params = parseParameters(historyToken);
		if (params.size() == 0) {
			info.notAHistoryToken = true;
			return info;
		} else {
			lastHistoryToken = historyToken;
		}
		if (params.containsKey(TAB_KEY)) {
			info.tabName = params.get(TAB_KEY);
			info.type = HistoryEventType.TABBED;
		}
		if (params.containsKey(TAB_SUB_KEY)) {
			info.subTabName = params.get(TAB_SUB_KEY);
		}
		if (params.containsKey(CONTENT_KEY)) {
			info.contentToken = params.get(CONTENT_KEY);
		}
		if (params.containsKey(NO_HISTORY_KEY)) {
			info.noHistory = true;
		}
		if (params.containsKey(ACTION_KEY)) {
			info.actionName = params.get(ACTION_KEY);
		}
		if (params.containsKey(CLASS_NAME_KEY)) {
			info.className = params.get(CLASS_NAME_KEY);
		}
		if (params.containsKey(ID_KEY)) {
			info.id = Long.parseLong(params.get(ID_KEY));
		}
		if (params.containsKey(LOCAL_ID_KEY)) {
			info.localId = Long.parseLong(params.get(LOCAL_ID_KEY));
		}
		if (params.containsKey(Y_KEY)) {
			info.y = Integer.parseInt(params.get(Y_KEY));
		}
		if (params.containsKey(SEARCH_INDEX) && params.containsKey(SEARCH_PAGE)) {
			int defId = Integer.parseInt(params.get(SEARCH_INDEX));
			int pageNumber = Integer.parseInt(params.get(SEARCH_PAGE));
			info.searchHistoryInfo = new SearchHistoryInfo(defId, pageNumber);
		}
		return info;
	}

	public static void register(AlcinaHistory subclass) {
		theInstance = subclass;
	}

	public void setLastHistoryToken(String lastHistoryToken) {
		this.lastHistoryToken = lastHistoryToken;
	}

	public void setNoHistoryDisabled(boolean noHistoryDisabled) {
		this.noHistoryDisabled = noHistoryDisabled;
	}

	public String toHash(Map<String, Object> params) {
		StringBuffer sb = new StringBuffer();
		ArrayList<String> keys = new ArrayList<String>(params.keySet());
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
			sb.append(params.get(k).toString());
		}
		return sb.toString();
	}

	public String tokenForSearch(SearchDefinition def, int pageNumber) {
		return CommonUtils.format("%1=%2&%3=%4", SEARCH_INDEX, def
				.getClientSearchIndex(), SEARCH_PAGE, pageNumber);
	}

	protected void initTokenDisplayNames() {
	}

	public enum HistoryEventType {
		NO_TAB_SPEC, UNTABBED, TABBED, SEARCH
	}

	public static class HistoryInfoBase {
		public boolean notAHistoryToken = false;

		public String contentToken = null;

		public String tabName = null;

		public String subTabName = null;

		public SearchHistoryInfo searchHistoryInfo = null;

		public String actionName = null;

		public long localId;

		public long id;

		public int y;

		public String className;

		public boolean noHistory = false;

		public HistoryEventType type = HistoryEventType.NO_TAB_SPEC;

		protected Map<String, Object> params;

		public String getToken() {
			params = new HashMap<String, Object>();
			params.put(TAB_KEY, tabName);
			params.put(TAB_SUB_KEY, subTabName);
			params.put(ACTION_KEY, actionName);
			params.put(CONTENT_KEY, contentToken);
			if (y != 0) {
				params.put(Y_KEY, y);
			}
			if (localId != 0 || id != 0) {
				params.put(LOCAL_ID_KEY, localId);
				params.put(ID_KEY, id);
			}
			if (noHistory) {
				params.put(NO_HISTORY_KEY, true);
			}
			params.put(CLASS_NAME_KEY, className);
			if (searchHistoryInfo != null) {
				params.put(SEARCH_INDEX, searchHistoryInfo.defId);
				params.put(SEARCH_PAGE, searchHistoryInfo.pageNumber);
			}
			addToToken(params);
			return AlcinaHistory.get().toHash(params);
		}

		public void setReferencedObject(HasIdAndLocalId hili) {
			className = hili.getClass().getName();
			id = hili.getId();
			localId = hili.getLocalId();
		}
		@SuppressWarnings("unchecked")
		public Object getReferencedObjectOrClassName() {
			if (className == null) {
				return null;
			}
			if (id == 0 && localId == 0) {
				return className;
			}
			return TransformManager.get().getObject(
					CommonLocator.get().classLookup().getClassForName(
							className), id, localId);
		}

		public List<SimpleHistoryEventInfo> toSimpleEvents() {
			List<SimpleHistoryEventInfo> result = new ArrayList<SimpleHistoryEventInfo>();
			String s = subTabName;
			subTabName = null;
			{
				SimpleHistoryEventInfo info = new SimpleHistoryEventInfo();
				info.displayName = AlcinaHistory.get().getTokenDisplayName(
						tabName);
				info.displayName = info.displayName == null ? CommonUtils
						.upperCaseFirstLetterOnly(tabName) : info.displayName;
				info.historyToken = getToken();
				result.add(info);
			}
			subTabName = s;
			if (subTabName != null) {
				SimpleHistoryEventInfo info = new SimpleHistoryEventInfo();
				info.displayName = AlcinaHistory.get().getTokenDisplayName(
						subTabName);
				info.displayName = info.displayName == null ? subTabName
						: info.displayName;
				info.historyToken = getToken();
				result.add(info);
			}
			return result;
		}

		protected void addToToken(Map<String, Object> params) {
		}
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
			this.displayName = displayName;
			historyToken = "";
		}
	}
}

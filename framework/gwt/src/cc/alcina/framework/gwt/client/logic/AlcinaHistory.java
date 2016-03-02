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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;
import cc.alcina.framework.gwt.client.util.Base64Utils;

import com.google.gwt.user.client.History;

/**
 * 
 * @author Nick Reddel
 */
public abstract class AlcinaHistory<I extends AlcinaHistoryItem> {
	private static final String SLASH_MARKER = "**__**__SLASH__**__**";

	// for testing - FF dev mode does some weird double-unencoding
	public static final String BASE64_PREFIX = "__b64__";

	private static final String DOUBLE_AMP = "%26%26";

	public static final String LOGIN_EVENT = "LOGIN_EVENT";

	public static final String SEARCH = "SEARCH";

	public static final String LOCATION_KEY = "t";

	public static final String CONTENT_KEY = "c";

	public static final String SEARCH_INDEX = "sdi";

	public static final String SEARCH_PAGE = "sdp";

	public static final String SEARCH_SERIALIZED = "sds";

	public static final String NO_HISTORY_KEY = "nh";

	public static final String ACTION_KEY = "a";

	public static final String ID_KEY = "id";

	public static final String LOCAL_ID_KEY = "lid";

	public static final String CLASS_NAME_KEY = "cn";

	public static final String Y_KEY = "y";

	public static final String PRE_HISTORY_KEY = "ph";

	public static AlcinaHistory get() {
		return Registry.impl(AlcinaHistory.class);
	}

	private boolean noHistoryDisabled = false;

	protected I currentEvent;

	protected I lastEvent;

	protected String lastHistoryToken;

	protected Map<String, String> tokenDisplayNames = new HashMap<String, String>();

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

	public I getCurrentEventOrEmpty() {
		return this.currentEvent != null ? this.currentEvent
				: parseToken(History.getToken());
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
		if (historyToken.startsWith("#")) {
			historyToken = historyToken.substring(1);
		}
		if (historyToken.startsWith("!")) {
			historyToken = historyToken.substring(1);
		}
		if (historyToken.startsWith("/")) {
			historyToken = historyToken.substring(1);
		}
		I item = createHistoryInfo();
		Map<String, String> params = item.parseParameters(historyToken);
		if (params.size() == 0) {
			item.notAHistoryToken = true;
			return item;
		} else {
			lastHistoryToken = historyToken;
		}
		if (CommonUtils.isNotNullOrEmpty(item.getTabName())) {
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

	static UrlComponentEncoder encoder;

	/**
	 * '&' in values is encoded as &&, to allow for hotmail escaping '&' in the
	 * hash as a whole - see fromHash
	 */
	public static String toHash(Map<String, String> params) {
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
			sb.append(encode(params.get(k).toString()));
		}
		return sb.toString();
	}

	public static String encode(String string) {
		if (encoder == null) {
			encoder = Registry.impl(UrlComponentEncoder.class);
		}
		string = string.replace("&", "&&");
		String encoded = encoder.encode(string);
		return encoded;
	}

	public static String maybeUnencode(String s) {
		if (s.startsWith(BASE64_PREFIX)) {
			try {
				s = new String(Base64Utils.fromBase64(
						s.substring(BASE64_PREFIX.length())), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return s;
	}

	public static StringMap fromHash(String s) {
		s = maybeUnencode(s);
		StringMap map = new StringMap();
		String key = null;
		String value = null;
		boolean forKey = true;
		for (int idx = 0; idx < s.length();) {
			if (forKey) {
				int idx1 = s.indexOf("=", idx);
				if (idx1 == -1) {
					break;
				} else {
					key = s.substring(idx, idx1);
					idx = idx1 + 1;
					forKey = false;
				}
			} else {
				// terminator index
				int idx0 = -1;
				int idxStart = idx;
				while (true) {
					int idx1 = s.indexOf("&", idx);
					// url encoding of '&'
					int idx2 = s.indexOf("%26", idx);
					// double-enc of '&' - i.e. part of a value, not a separator
					int idx3 = s.indexOf(DOUBLE_AMP, idx);
					// do we have a terminator?
					if (idx1 == -1 && idx2 == -1) {
						idx0 = s.length();
						break;//
					} else {
						idx0 = idx1 == -1 ? idx2
								: idx2 == -1 ? idx1 : Math.min(idx1, idx2);
						if (idx0 < idx3 || idx3 == -1) {
							break;// found terminator
						}
						idx = idx3 + DOUBLE_AMP.length();
					}
				}
				map.put(key,
						Registry.impl(UrlComponentEncoder.class)
								.decode(s.substring(idxStart, idx0))
								.replace("&&", "&"));
				forKey = true;
				idx = idx0;
				idx += s.indexOf("&", idx) == idx ? 1 : 3;// advance past setp
			}
		}
		return map;
	}

	public String tokenForSearch(SearchDefinition def, int pageNumber) {
		return tokenForSearch(def, pageNumber, null);
	}

	public String tokenForSearch(SearchDefinition def, int pageNumber,
			String searchDefinitionSerialized) {
		AlcinaHistoryItem hib = createHistoryInfo();
		hib.setSearchHistoryInfo(
				new SearchHistoryInfo(def.getClientSearchIndex(), pageNumber,
						searchDefinitionSerialized));
		return hib.toTokenString();
	}

	protected void initTokenDisplayNames() {
	}

	public static void initialiseDebugIds() {
		String token = History.getToken();
		if (token != null) {
			AlcinaHistoryItem currentEvent = AlcinaHistory.get()
					.parseToken(token);
			if (currentEvent != null) {
				for (String dbgId : AlcinaDebugIds.DEBUG_IDS) {
					if (currentEvent.hasParameter(dbgId)) {
						AlcinaDebugIds.setFlag(dbgId);
					}
				}
			}
		}
	}

	public enum HistoryEventType {
		NO_TAB_SPEC, UNTABBED, TABBED
	}

	public static class SearchHistoryInfo {
		public int defId;

		public int pageNumber;

		public String searchDefinitionSerialized;

		public SearchHistoryInfo(int defId, int pageNumber,
				String searchDefinitionSerialized) {
			this.defId = defId;
			this.pageNumber = pageNumber;
			this.searchDefinitionSerialized = searchDefinitionSerialized;
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

	public void maybeRemove(String key) {
		I current = getCurrentEventOrEmpty();
		String s1 = current.toTokenString();
		current.removeParameter(key);
		String s2 = current.toTokenString();
		if (!s2.equals(s1)) {
			History.newItem(s2);
		}
	}
}

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

import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.HistoryEventType;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SearchHistoryInfo;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;
import cc.alcina.framework.gwt.client.util.Base64Utils;

public class AlcinaHistoryItem {
	public boolean notAHistoryToken;

	public AlcinaHistory.HistoryEventType type = HistoryEventType.NO_TAB_SPEC;

	protected Map<String, String> params = new HashMap<String, String>();

	protected void addToToken(Map<String, Object> params) {
	}

	public <T extends AlcinaHistoryItem> T copy() {
		AlcinaHistoryItem item = AlcinaHistory.get().createHistoryInfo();
		item.params.putAll(params);
		item.notAHistoryToken = notAHistoryToken;
		item.type = type;
		return (T) item;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AlcinaHistoryItem) {
			AlcinaHistoryItem o = (AlcinaHistoryItem) obj;
			return CommonUtils.equals(obj.getClass(), getClass(), o.params,
					params, o.notAHistoryToken, notAHistoryToken, o.type, type);
		}
		return false;
	}

	public String getActionName() {
		return getStringParameter(ACTION_KEY);
	}

	public boolean getBooleanParameter(String key) {
		String value = params.get(key);
		return value == null ? false
				: value.equals("t") || Boolean.parseBoolean(value);
	}

	public String getClassName() {
		return getStringParameter(CLASS_NAME_KEY);
	}

	public String getContentToken() {
		return getStringParameter(CONTENT_KEY);
	}

	public long getId() {
		return getLongParameter(ID_KEY);
	}

	public int getIntParameter(String key) {
		String value = params.get(key);
		return value == null ? 0 : CommonUtils.friendlyParseInt(value);
	}

	public long getLocalId() {
		return getLongParameter(LOCAL_ID_KEY);
	}

	public String getLocation() {
		return getStringParameter(LOCATION_KEY);
	}

	protected String getLocationPart(int idx) {
		List<String> parts = getLocationParts();
		return idx < parts.size() ? parts.get(idx) : null;
	}

	private List<String> getLocationParts() {
		String[] locs = CommonUtils.nullToEmpty(getLocation()).split("\\*");
		return new ArrayList<String>(Arrays.asList(locs));
	}

	public long getLongParameter(String key) {
		String value = params.get(key);
		return value == null || value.isEmpty() ? 0
				: CommonUtils.friendlyParseLong(value);
	}

	public String getPreHistory() {
		return getStringParameter(PRE_HISTORY_KEY);
	}

	public SearchHistoryInfo getSearchHistoryInfo() {
		if (!hasParameter(SEARCH_SERIALIZED)) {
			return null;
		}
		return new SearchHistoryInfo(getStringParameter(SEARCH_SERIALIZED),
				getStringParameter(SEARCH_MARKER));
	}

	public String getStringParameter(String key) {
		return params.get(key);
	}

	public String getSubTabName() {
		return getLocationPart(1);
	}

	public String getTabName() {
		return getLocationPart(0);
	}

	public int getY() {
		return getIntParameter(Y_KEY);
	}

	public boolean hasParameter(String key) {
		return params.containsKey(key);
	}

	public boolean isNoHistory() {
		return getBooleanParameter(NO_HISTORY_KEY);
	}

	public Map<String, String> parseParameters(String s,
			BiPredicate<String, String> nonDecoder) {
		params = AlcinaHistory.fromHash(s, nonDecoder);
		return params;
	}

	public String removeParameter(String key) {
		return params.remove(key);
	}

	public void setActionName(String actionName) {
		setParameter(ACTION_KEY, actionName);
	}

	public void setClassName(String className) {
		setParameter(CLASS_NAME_KEY,
				CommonUtils.nullToEmpty(className).replace("$", "."));
	}

	public void setContentToken(String contentToken) {
		setParameter(CONTENT_KEY, contentToken);
	}

	public void setId(long id) {
		setParameter(ID_KEY, id);
	}

	public void setLocalId(long localId) {
		setParameter(LOCAL_ID_KEY, localId);
	}

	public void setLocation(String location) {
		setParameter(LOCATION_KEY, location);
	}

	protected void setLocationPart(int idx, String name) {
		List<String> parts = getLocationParts();
		for (int i = 0; i <= idx; i++) {
			if (i == parts.size()) {
				parts.add(i, "");// clearer semantics
			}
		}
		parts.set(idx, name);
		for (int i = parts.size() - 1; i >= 0; i--) {
			if (CommonUtils.isNullOrEmpty(parts.get(i))) {
				parts.remove(i);
			}
		}
		setLocation(CommonUtils.join(parts, "*"));
	}

	public void setLocationParts(String... parts) {
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			setLocationPart(i, part);
		}
	}

	public void setNoHistory(boolean noHistory) {
		setParameter(NO_HISTORY_KEY, noHistory);
	}

	public void setParameter(String key, Object value) {
		if (value instanceof Number) {
			if (((Number) value).longValue() == 0) {
				value = null;
			}
		}
		if (value instanceof Boolean) {
			if (((Boolean) value) == false) {
				value = null;
			}
		}
		params.put(key, value == null ? null : value.toString());
	}

	public void setParameter(String key, Object value, boolean explicitBlanks) {
		params.put(key, value == null ? null : value.toString());
	}

	public void setPreHistory(String preHistoryName) {
		setParameter(PRE_HISTORY_KEY, preHistoryName);
	}

	public void setSearchHistoryInfo(SearchHistoryInfo searchHistoryInfo) {
		if (searchHistoryInfo.searchDefinitionSerialized != null) {
			setParameter(SEARCH_SERIALIZED,
					searchHistoryInfo.searchDefinitionSerialized);
			setParameter(SEARCH_MARKER,
					searchHistoryInfo.searchDefinitionMarker);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public void setSubTabName(String subTabName) {
		setLocationPart(1, subTabName);
	}

	public void setTabName(String tabName) {
		setLocationPart(0, tabName);
	}

	public void setY(int y) {
		setParameter(Y_KEY, y);
	}

	public String toBase64TokenString() {
		try {
			return AlcinaHistory.BASE64_PREFIX
					+ Base64Utils.toBase64(toTokenString().getBytes("UTF-8"));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public String toHref() {
		return "#" + toTokenString();
	}

	public List<AlcinaHistory.SimpleHistoryEventInfo> toSimpleEvents() {
		List<AlcinaHistory.SimpleHistoryEventInfo> result = new ArrayList<AlcinaHistory.SimpleHistoryEventInfo>();
		String s = getSubTabName();
		setSubTabName(null);
		{
			AlcinaHistory.SimpleHistoryEventInfo info = new SimpleHistoryEventInfo();
			info.displayName = AlcinaHistory.get()
					.getTokenDisplayName(getTabName());
			info.displayName = info.displayName == null
					? CommonUtils.upperCaseFirstLetterOnly(getTabName())
					: info.displayName;
			info.historyToken = toTokenString();
			result.add(info);
		}
		setSubTabName(s);
		if (getSubTabName() != null) {
			AlcinaHistory.SimpleHistoryEventInfo info = new SimpleHistoryEventInfo();
			info.displayName = AlcinaHistory.get()
					.getTokenDisplayName(getSubTabName());
			info.displayName = info.displayName == null ? getSubTabName()
					: info.displayName;
			info.historyToken = toTokenString();
			result.add(info);
		}
		return result;
	}

	@Override
	public String toString() {
		return Ax.format("%s\nNot a history token: %s Type: %s", params,
				notAHistoryToken, type);
	}

	public String toTokenString() {
		return AlcinaHistory.toHash(params);
	}
}
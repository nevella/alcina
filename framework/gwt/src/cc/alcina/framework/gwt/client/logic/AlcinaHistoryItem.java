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

import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.ACTION_KEY;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.CLASS_NAME_KEY;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.CONTENT_KEY;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.ID_KEY;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.LOCAL_ID_KEY;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.NO_HISTORY_KEY;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.SEARCH_INDEX;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.SEARCH_PAGE;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.TAB_KEY;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.TAB_SUB_KEY;
import static cc.alcina.framework.gwt.client.logic.AlcinaHistory.Y_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.http.client.URL;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.HistoryEventType;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SearchHistoryInfo;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;

public class AlcinaHistoryItem {
	public boolean notAHistoryToken;

	public AlcinaHistory.HistoryEventType type = HistoryEventType.NO_TAB_SPEC;

	private Map<String, String> params = new HashMap<String, String>();

	public String getActionName() {
		return getStringParameter(ACTION_KEY);
	}

	public boolean getBooleanParameter(String key) {
		String value = params.get(key);
		return value == null ? false : Boolean.parseBoolean(value);
	}

	public Map<String, String> parseParameters(String s) {
		params = new HashMap<String, String>();
		String[] pairs = s.split("&");
		for (String pair : pairs) {
			String[] split = pair.split("=");
			if (split.length == 2) {
				params.put(split[0], URL.decodeQueryString(split[1]));
			}
		}
		return params;
	}

	public boolean hasParameter(String key) {
		return params.containsKey(key);
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
		return value == null ? 0 : Integer.parseInt(value);
	}

	public long getLocalId() {
		return getLongParameter(LOCAL_ID_KEY);
	}

	public long getLongParameter(String key) {
		String value = params.get(key);
		return value == null ? 0 : Long.parseLong(value);
	}

	@SuppressWarnings("unchecked")
	public Object getReferencedObjectOrClassName() {
		if (getClassName() == null) {
			return null;
		}
		if (getId() == 0 && getLocalId() == 0) {
			return getClassName();
		}
		return TransformManager.get().getObject(
				CommonLocator.get().classLookup().getClassForName(
						getClassName()), getId(), getLocalId());
	}

	public AlcinaHistory.SearchHistoryInfo getSearchHistoryInfo() {
		if (!hasParameter(SEARCH_INDEX)) {
			return null;
		}
		int pageNumber = getIntParameter(SEARCH_PAGE);
		return new SearchHistoryInfo(getIntParameter(SEARCH_INDEX), pageNumber);
	}

	public String getStringParameter(String key) {
		return params.get(key);
	}

	public String getSubTabName() {
		return getStringParameter(TAB_SUB_KEY);
	}

	public String getTabName() {
		return getStringParameter(TAB_KEY);
	}

	public String toTokenString() {
		return AlcinaHistory.get().toHash(params);
	}

	public int getY() {
		return getIntParameter(Y_KEY);
	}

	public boolean isNoHistory() {
		return getBooleanParameter(NO_HISTORY_KEY);
	}

	public void setActionName(String actionName) {
		setParameter(ACTION_KEY, actionName);
	}

	public void setClassName(String className) {
		setParameter(CLASS_NAME_KEY, className);
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

	public void setNoHistory(boolean noHistory) {
		setParameter(NO_HISTORY_KEY, noHistory);
	}

	public void setParameter(String key, Object value, boolean explicitBlanks) {
		params.put(key, value == null ? null : value.toString());
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

	public void setReferencedObject(HasIdAndLocalId hili) {
		setParameter(CLASS_NAME_KEY, hili.getClass().getName());
		setParameter(ID_KEY, hili.getId());
		setParameter(LOCAL_ID_KEY, hili.getLocalId());
	}

	public void setSearchHistoryInfo(
			AlcinaHistory.SearchHistoryInfo searchHistoryInfo) {
		if (searchHistoryInfo != null) {
			setParameter(SEARCH_INDEX, searchHistoryInfo.defId,true);
			setParameter(SEARCH_PAGE, searchHistoryInfo.pageNumber);
		}
	}

	public void setSubTabName(String subTabName) {
		setParameter(TAB_SUB_KEY, subTabName);
	}

	public void setTabName(String tabName) {
		setParameter(TAB_KEY, tabName);
	}

	public void setY(int y) {
		setParameter(Y_KEY, y);
	}

	public List<AlcinaHistory.SimpleHistoryEventInfo> toSimpleEvents() {
		List<AlcinaHistory.SimpleHistoryEventInfo> result = new ArrayList<AlcinaHistory.SimpleHistoryEventInfo>();
		String s = getSubTabName();
		setSubTabName(null);
		{
			AlcinaHistory.SimpleHistoryEventInfo info = new SimpleHistoryEventInfo();
			info.displayName = AlcinaHistory.get().getTokenDisplayName(
					getTabName());
			info.displayName = info.displayName == null ? CommonUtils
					.upperCaseFirstLetterOnly(getTabName()) : info.displayName;
			info.historyToken = toTokenString();
			result.add(info);
		}
		setSubTabName(s);
		if (getSubTabName() != null) {
			AlcinaHistory.SimpleHistoryEventInfo info = new SimpleHistoryEventInfo();
			info.displayName = AlcinaHistory.get().getTokenDisplayName(
					getSubTabName());
			info.displayName = info.displayName == null ? getSubTabName()
					: info.displayName;
			info.historyToken = toTokenString();
			result.add(info);
		}
		return result;
	}

	protected void addToToken(Map<String, Object> params) {
	}
}
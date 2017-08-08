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
package cc.alcina.framework.gwt.client.gwittir.customiser;

import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.provider.ExpandableStringLabelProvider;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class ExpandableLabelCustomiser implements Customiser {
	public static final String MAX_WIDTH = "maxLabelWidth";

	public static final String FORCE_COLUMN_WIDTH = "forceColumnWidth";

	public static final String SHOW_NEWLINES_AS_HTML_BREAKS = "showNewlinesAsBreaks";

	public static final String SHOW_AS_POPUP = "SHOW_AS_POPUP";

	public static final String ESCAPE_HTML = "ESCAPE_HTML";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		NamedParameter p = NamedParameter.Support
				.getParameter(info.parameters(), MAX_WIDTH);
		int maxLength = p == null ? GwittirBridge.MAX_EXPANDABLE_LABEL_LENGTH
				: p.intValue();
		p = NamedParameter.Support.getParameter(info.parameters(),
				FORCE_COLUMN_WIDTH);
		boolean forceColumnWidth = p == null ? true : p.booleanValue();
		p = NamedParameter.Support.getParameter(info.parameters(),
				SHOW_NEWLINES_AS_HTML_BREAKS);
		boolean showNewlinesAsBreaks = p == null ? true : p.booleanValue();
		p = NamedParameter.Support.getParameter(info.parameters(),
				SHOW_AS_POPUP);
		boolean showAsPopup = p == null ? false : p.booleanValue();
		p = NamedParameter.Support.getParameter(info.parameters(), ESCAPE_HTML);
		boolean escapeHtml = p == null ? true : p.booleanValue();
		return new ExpandableStringLabelProvider(maxLength, forceColumnWidth,
				showNewlinesAsBreaks, showAsPopup,escapeHtml);
	}
}

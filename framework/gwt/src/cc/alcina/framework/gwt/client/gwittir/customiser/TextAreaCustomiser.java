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

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.widget.MultilineLabel;
import cc.alcina.framework.gwt.client.gwittir.widget.TextArea;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Label;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class TextAreaCustomiser implements Customiser {
	public static final String WIDTH = "width";

	public static final String LINES = "lines";

	public static final String NON_EDITABLE_AS_LABEL = "non-editable-as-label";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, CustomiserInfo info) {
		NamedParameter param = NamedParameter.Support.getParameter(
				info.parameters(), WIDTH);
		int width = param == null ? 0 : param.intValue();
		param = NamedParameter.Support.getParameter(info.parameters(), LINES);
		int lines = param == null ? 4 : param.intValue();
		param = NamedParameter.Support.getParameter(info.parameters(),
				NON_EDITABLE_AS_LABEL);
		boolean neal = param == null ? false : param.booleanValue();
		return new TextAreaProvider(editable, width, lines, neal);
	}

	public static class TextAreaProvider implements BoundWidgetProvider {
		private final boolean editable;

		private final int width;

		private final int lines;

		private boolean neal;

		public TextAreaProvider(boolean editable, int width, int lines,
				boolean neal) {
			this.editable = editable;
			this.width = width;
			this.lines = lines;
			this.neal = neal;
		}

		public BoundWidget get() {
			if (neal && !editable) {
				MultilineLabel l = new MultilineLabel();
				l.setWidth(width + "px");
				return l;
			}
			TextArea area = new TextArea();
			area.setVisibleLines(lines);
			area.setReadOnly(!editable);
			if (width != 0) {
				area.setWidth(width + "px");
			}
			return area;
		}
	}
}

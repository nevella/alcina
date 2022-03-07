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

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.gwittir.widget.MultilineLabel;
import cc.alcina.framework.gwt.client.gwittir.widget.TextArea;

@Reflected
/**
 *
 * @author Nick Reddel
 */
public class TextAreaCustomiser implements Customiser {
	public static final String WIDTH = "width";

	public static final String LINES = "lines";

	public static final String HINT = "hint";

	public static final String NON_EDITABLE_AS_LABEL = "non-editable-as-label";

	public static final String ENSURE_ALL_LINES_VISIBLE = "ENSURE_ALL_LINES_VISIBLE";

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		NamedParameter param = NamedParameter.Support
				.getParameter(info.parameters(), WIDTH);
		int width = param == null ? 0 : param.intValue();
		param = NamedParameter.Support.getParameter(info.parameters(), LINES);
		int lines = param == null ? 4 : param.intValue();
		param = NamedParameter.Support.getParameter(info.parameters(),
				NON_EDITABLE_AS_LABEL);
		boolean neal = param == null ? false : param.booleanValue();
		return new TextAreaProvider(editable, width, lines, neal,
				NamedParameter.Support.stringValue(info.parameters(), HINT,
						null),
				NamedParameter.Support.booleanValue(info.parameters(),
						ENSURE_ALL_LINES_VISIBLE));
	}

	public static class TextAreaProvider implements BoundWidgetProvider {
		private final boolean editable;

		private final int width;

		private final int lines;

		private boolean neal;

		private final String hint;

		private boolean ensureAllLinesVisible;

		public TextAreaProvider(boolean editable, int width, int lines,
				boolean neal, String hint, boolean ensureAllLinesVisible) {
			this.editable = editable;
			this.width = width;
			this.lines = lines;
			this.neal = neal;
			this.hint = hint;
			this.ensureAllLinesVisible = ensureAllLinesVisible;
		}

		@Override
		public BoundWidget get() {
			if (neal && !editable) {
				MultilineLabel l = new MultilineLabel();
				if (width != 0) {
					l.setWidth(width + "px");
				}
				return l;
			}
			TextArea area = new TextArea();
			area.setVisibleLines(lines);
			area.setReadOnly(!editable);
			if (Ax.notBlank(hint)) {
				area.setHint(hint);
			}
			if (width != 0) {
				area.setWidth(width + "px");
			}
			area.setEnsureAllLinesVisible(ensureAllLinesVisible);
			return area;
		}
	}
}

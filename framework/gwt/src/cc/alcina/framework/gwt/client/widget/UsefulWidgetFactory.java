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
package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public class UsefulWidgetFactory {
	public static final String BULLET_SEPARATOR_HTML = "\u00A0\u00A0\u00A0\u2022\u00A0\u00A0\u00A0";

	public static final String NARROW_BULLET_SEPARATOR_HTML = "\u00A0\u00A0\u2022\u00A0\u00A0";

	public static final String WIDE_BULLET_SEPARATOR_HTML = "\u00A0\u00A0\u00A0\u00A0\u2022\u00A0\u00A0\u00A0\u00A0";

	public static FlowPanel mediumTitleWidget(String title) {
		FlowPanel fp = new FlowPanel();
		fp.setStyleName("medium-title");
		Label l = new HTML(title);
		l.setStyleName("");
		fp.add(l);
		return fp;
	}

	public static FlowPanel lowTitleWidget(String title) {
		FlowPanel fp = new FlowPanel();
		fp.setStyleName("low-title");
		Label l = new HTML(title);
		l.setStyleName("");
		fp.add(l);
		return fp;
	}

	public static Widget boldInline(String text) {
		return new InlineHTML("<b>" + text + "</b>");
	}

	public static Widget italicInline(String text) {
		return new InlineHTML("<i>" + text + "</i>");
	}

	public static HTML createEmptyLabel() {
		return new HTML("&nbsp;");
	}

	public static InlineHTML createSpacer(int spaces) {
		String s = "";
		for (; spaces > 0; spaces--) {
			s += "&nbsp;";
		}
		return new InlineHTML(s);
	}

	public static Widget createBulletSeparator() {
		return new InlineHTML(BULLET_SEPARATOR_HTML);
	}

	public static Widget createNarrowBulletSeparator() {
		return new InlineHTML(NARROW_BULLET_SEPARATOR_HTML);
	}

	public static Label formatLabel(String template, Object... params) {
		return new Label(CommonUtils.formatJ(template, params));
	}

	public static FlowPanel styledPanel(String cssClassName) {
		FlowPanel panel = new FlowPanel();
		panel.setStyleName(cssClassName);
		return panel;
	}

	public static SimplePanel styledSimplePanel(Widget child,
			String cssClassName) {
		SimplePanel panel = new SimplePanel(child);
		panel.setStyleName(cssClassName);
		return panel;
	}

	public static Label styledLabel(String text, String cssClassName) {
		Label label = new Label(text);
		label.setStyleName(cssClassName);
		return label;
	}
}

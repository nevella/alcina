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

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TabBar;

/**
 *
 * @author Nick Reddel
 */

 public class StyleableTabBar extends TabBar {
	private HorizontalPanel panel;

	public StyleableTabBar() {
		super();
		panel = (HorizontalPanel) getWidget();
	}

	public void makeRightAligned() {
		panel.setCellWidth(panel.getWidget(0), "100%");
		panel.setCellWidth(panel.getWidget(1), "1px");
	}

	public void updateLRStyles() {
		if (panel.getWidgetCount() > 3) {
			panel.getWidget(1).addStyleName("tabBarLeft");
			panel.getWidget(panel.getWidgetCount() - 2).addStyleName(
					"tabBarRight");
		}
	}
}

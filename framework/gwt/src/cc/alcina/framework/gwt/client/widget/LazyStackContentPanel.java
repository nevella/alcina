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

import java.util.List;

import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;

import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public abstract class LazyStackContentPanel extends LazyPanel<Widget> {
	private boolean initalisingLayout;

	public abstract List<SimpleHistoryEventInfo> getHistoryEventInfo() ;
	public abstract String getHistoryToken();

	public abstract String getDisplayName();

	public void setInitalisingLayout(boolean initalisingLayout) {
		this.initalisingLayout = initalisingLayout;
	}

	public boolean isInitalisingLayout() {
		return initalisingLayout;
	}

	public void setVisible(boolean visible) {
		if (visible && !initalisingLayout) {
			ensureWidget();
		}
		initalisingLayout = false;
		super.setVisible(visible);
	}

	public abstract String getSubTabName() ;

	public static class LazyStackPanel extends StackPanel {
		private boolean initalisingLayout;

		public boolean isInitalisingLayout() {
			return this.initalisingLayout;
		}

		public void setInitalisingLayout(boolean initalisingLayout) {
			this.initalisingLayout = initalisingLayout;
		}

		@Override
		public void insert(Widget w, int beforeIndex) {
			if (w instanceof LazyStackContentPanel && initalisingLayout) {
				((LazyStackContentPanel) w).setInitalisingLayout(true);
			}
			super.insert(w, beforeIndex);
		}
	}
}

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

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.LoginStateVisible;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.widget.VisibilityChangeEvent.Handler;
import cc.alcina.framework.gwt.client.widget.VisibilityChangeEvent.HasVisibilityChangeHandlers;

/**
 *
 * @author Nick Reddel
 */
public class BaseTab extends Composite
		implements Permissible, LoginStateVisible, HasVisibilityChangeHandlers {
	protected AccessLevel minimumAccessLevel = AccessLevel.EVERYONE;

	protected ScrollPanel scroller;

	protected boolean wasSelected;

	protected String name;

	protected String displayName;

	public AccessLevel accessLevel() {
		return this.minimumAccessLevel;
	}

	@Override
	public HandlerRegistration addVisibilityChangeHandler(Handler handler) {
		return addHandler(handler, VisibilityChangeEvent.TYPE);
	}

	public String getDisplayName() {
		return this.displayName != null ? this.displayName : this.name;
	}

	public String getHistoryToken() {
		return "";
	}

	public String getName() {
		return this.name;
	}

	public Widget getPageWidget() {
		return super.getWidget();
	}

	public ScrollPanel getScroller() {
		return this.scroller;
	}

	public Toolbar getToolbar() {
		return null;
	}

	public String rule() {
		return "";
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			tabSelected();
		}
		fireEvent(new VisibilityChangeEvent(visible));
	}

	public void tabSelected() {
		if (!wasSelected) {
			wasSelected = true;
			ensureWidget();
		}
	}

	public boolean visibleForLoginState(LoginState state) {
		return true;
	}

	protected void ensureWidget() {
	}
}

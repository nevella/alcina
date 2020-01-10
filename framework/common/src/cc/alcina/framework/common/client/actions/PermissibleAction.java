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
package cc.alcina.framework.common.client.actions;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.Vetoer;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissible;

/**
 * 
 * @author Nick Reddel
 */
public class PermissibleAction implements Permissible {
	private String displayName;

	private String cssClassName;

	private String actionName;

	public PermissibleAction() {
	}

	public PermissibleAction(String displayName, String actionName) {
		this(displayName, actionName, null);
	}

	public PermissibleAction(String displayName, String actionName,
			String cssClassName) {
		this.actionName = actionName;
		this.displayName = displayName;
		this.cssClassName = cssClassName;
	}

	public AccessLevel accessLevel() {
		return AccessLevel.LOGGED_IN;
	}

	public String getActionGroupName() {
		return null;
	}

	public String getActionName() {
		return this.actionName;
	}

	public String getCssClassName() {
		return cssClassName;
	}

	public List<Vetoer> getDefaultVetoers() {
		return null;
	}

	public String getDisplayName() {
		return displayName != null ? displayName : getActionName();
	}

	public String rule() {
		return null;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public interface HasPermissibleActionChildren {
		public abstract List<PermissibleAction> getChildren();
	}

	public interface HasPermissibleActionDelegate {
		public abstract PermissibleAction getDelegate();
	}

	public static class PermissibleActionEveryone extends PermissibleAction {
		@Override
		public AccessLevel accessLevel() {
			return AccessLevel.EVERYONE;
		}
	}

	public static class PermissibleActionWithChildrenAndDelegate
			extends PermissibleActionWithDelegate
			implements HasPermissibleActionChildren {
		private List<PermissibleAction> children = new ArrayList<PermissibleAction>();

		public PermissibleActionWithChildrenAndDelegate(
				PermissibleAction delegate) {
			super(delegate);
		}

		public List<PermissibleAction> getChildren() {
			return this.children;
		}
	}

	public static class PermissibleActionWithDelegate extends PermissibleAction
			implements HasPermissibleActionDelegate {
		private final PermissibleAction delegate;

		public PermissibleActionWithDelegate(PermissibleAction delegate) {
			this.delegate = delegate;
		}

		public AccessLevel accessLevel() {
			return this.delegate.accessLevel();
		}

		public String getActionName() {
			return this.delegate.getActionName();
		}

		public String getCssClassName() {
			return this.delegate.getCssClassName();
		}

		@Override
		public PermissibleAction getDelegate() {
			return this.delegate;
		}

		public String getDisplayName() {
			return this.delegate.getDisplayName();
		}

		public String rule() {
			return this.delegate.rule();
		}

		public void setActionName(String actionName) {
			this.delegate.setActionName(actionName);
		}
	}

	public void wasCalled() {
	}
}

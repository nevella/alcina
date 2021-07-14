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

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.flat.PropertySerialization;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 * 
 *         FIXME - dirndl.2 - all these UI properties (just make them transient
 *         getters)
 */
@TypeSerialization(properties = {
		@PropertySerialization(ignore = true, name = "actionName"),
		@PropertySerialization(ignore = true, name = "cssClassName"),
		@PropertySerialization(ignore = true, name = "displayName") })
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

	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.LOGGED_IN;
	}

	public String getActionName() {
		return this.actionName;
	}

	// FIXME - dirndl.2 - remove
	public String getCssClassName() {
		return cssClassName;
	}

	public String getDescription() {
		return "";
	}

	@PropertySerialization(ignore = true)
	public String getDisplayName() {
		return displayName != null ? displayName : getActionName();
	}

	public String provideId() {
		if (Ax.notBlank(getActionName())) {
			return getActionName();
		}
		return CommonUtils.restId(
				getClass().getSimpleName().replaceFirst("(.+)Action", "$1"));
	}

	@Override
	public String rule() {
		return null;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void wasCalled() {
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

		@Override
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

		@Override
		public AccessLevel accessLevel() {
			return this.delegate.accessLevel();
		}

		@Override
		public String getActionName() {
			return this.delegate.getActionName();
		}

		@Override
		public String getCssClassName() {
			return this.delegate.getCssClassName();
		}

		@Override
		public PermissibleAction getDelegate() {
			return this.delegate;
		}

		@Override
		@AlcinaTransient
		public String getDisplayName() {
			return this.delegate.getDisplayName();
		}

		@Override
		public String rule() {
			return this.delegate.rule();
		}

		@Override
		public void setActionName(String actionName) {
			this.delegate.setActionName(actionName);
		}
	}
}

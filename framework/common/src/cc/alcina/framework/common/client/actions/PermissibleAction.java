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
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 *
 *
 * FIXME - dirndl 1x1d - all these UI properties (just make them transient
 * getters)
 *
 * Actually, some client-only subclasses use them...
 *
 * @author Nick Reddel
 */
public abstract class PermissibleAction implements Permissible {
	public PermissibleAction() {
	}

	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.LOGGED_IN;
	}

	@AlcinaTransient
	public abstract String getActionName();

	// FIXME - dirndl 1x1d - remove
	// Nope - actually (because these are passed around a bunch), allowing class
	// to be specified is a *good idea*. See DirndlDir, 'how close to the UI
	// layer is the model'. This is not to say cssClassName should be a field -
	// but it *should* be a property
	@AlcinaTransient
	public String getCssClassName() {
		return "";
	}

	@AlcinaTransient
	public String getDescription() {
		return "";
	}

	@AlcinaTransient
	public String getDisplayName() {
		return getActionName();
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

	public void wasCalled() {
	}

	public static class CancelAction extends PermissibleAction {
		public static final transient String CANCEL_ACTION = "Cancel";

		@Override
		public String getActionName() {
			return CANCEL_ACTION;
		}
	}

	public static class FinishAction extends PermissibleAction {
		private static final transient String FINISH = "Finish";

		@Override
		public String getActionName() {
			return FINISH;
		}
	}

	public interface HasPermissibleActionChildren {
		public abstract List<PermissibleAction> getChildren();
	}

	public interface HasPermissibleActionDelegate {
		public abstract PermissibleAction getDelegate();
	}

	public static class NextAction extends PermissibleAction {
		private static final transient String NEXT = "Next";

		@Override
		public String getActionName() {
			return NEXT;
		}

		@Override
		public String getDisplayName() {
			return "Next >";
		}
	}

	public static class OkAction extends PermissibleAction {
		public static final transient String OK_ACTION = "OK";

		@Override
		public String getActionName() {
			return OK_ACTION;
		}
	}

	public static abstract class PermissibleActionEveryone
			extends PermissibleAction {
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
	}

	public static class PreviousAction extends PermissibleAction {
		private static final transient String PREVIOUS = "Previous";

		@Override
		public String getActionName() {
			return PREVIOUS;
		}
	}
}

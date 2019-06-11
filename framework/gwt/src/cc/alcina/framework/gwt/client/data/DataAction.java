package cc.alcina.framework.gwt.client.data;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.instances.CreateAction;
import cc.alcina.framework.common.client.actions.instances.DeleteAction;
import cc.alcina.framework.common.client.actions.instances.EditAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;

public enum DataAction {
	VIEW {
		@Override
		public Class<? extends PermissibleAction> actionClass() {
			return ViewAction.class;
		}
	},
	EDIT {
		@Override
		public Class<? extends PermissibleAction> actionClass() {
			return EditAction.class;
		}

		@Override
		public boolean isEditable() {
			return true;
		}
	},
	DELETE {
		@Override
		public Class<? extends PermissibleAction> actionClass() {
			return DeleteAction.class;
		}
	},
	CREATE {
		@Override
		public Class<? extends PermissibleAction> actionClass() {
			return CreateAction.class;
		}

		@Override
		public boolean isEditable() {
			return true;
		}
	},
	PREVIEW {
		@Override
		public Class<? extends PermissibleAction> actionClass() {
			throw new UnsupportedOperationException();
		}
	};
	public abstract Class<? extends PermissibleAction> actionClass();

	public boolean isEditable() {
		return false;
	}
}
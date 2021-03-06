package cc.alcina.framework.gwt.client.ide;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;

public interface WorkspaceActionHandler {
	void performAction(PermissibleActionEvent event, Object node, Object object,
			Workspace workspace, Class nodeObjectClass);

	public static interface CloneActionHandler extends WorkspaceActionHandler {
	}

	public static interface CreateActionHandler extends WorkspaceActionHandler {
	}

	public static interface DeleteActionHandler extends WorkspaceActionHandler {
	}

	public static interface EditActionHandler extends WorkspaceActionHandler {
	}

	public static interface ViewActionHandler extends WorkspaceActionHandler {
	}
}

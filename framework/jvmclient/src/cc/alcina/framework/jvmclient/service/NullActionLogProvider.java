package cc.alcina.framework.jvmclient.service;

import java.util.List;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.ActionLogProvider;
import cc.alcina.framework.common.client.actions.RemoteAction;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class NullActionLogProvider implements ActionLogProvider {
	@Override
	public void getLogsForAction(RemoteAction action, int count,
			AsyncCallback<List<ActionLogItem>> outerCallback, boolean refresh) {
	}

	@Override
	public void insertLogForAction(RemoteAction action, ActionLogItem item) {
	}
}

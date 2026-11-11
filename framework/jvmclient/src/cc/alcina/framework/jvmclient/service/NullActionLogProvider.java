package cc.alcina.framework.jvmclient.service;

import java.util.List;

import cc.alcina.framework.common.client.util.AsyncCallback;

import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.gwt.client.ide.provider.ActionLogProvider;

public class NullActionLogProvider implements ActionLogProvider {
	@Override
	public void getLogsForAction(RemoteAction action, int count,
			AsyncCallback<List<JobTracker>> outerCallback, boolean refresh) {
	}
}

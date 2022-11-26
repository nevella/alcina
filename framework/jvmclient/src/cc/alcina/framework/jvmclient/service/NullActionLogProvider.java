package cc.alcina.framework.jvmclient.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.actions.ActionLogProvider;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.JobTracker;

public class NullActionLogProvider implements ActionLogProvider {
	@Override
	public void getLogsForAction(RemoteAction action, int count,
			AsyncCallback<List<JobTracker>> outerCallback, boolean refresh) {
	}
}

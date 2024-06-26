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
package cc.alcina.framework.gwt.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.ActionLogProvider;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.csobjects.JobTracker;

/**
 *
 * @author Nick Reddel
 */
public class StandardActionLogProvider implements ActionLogProvider {
	private Map<Class<? extends RemoteAction>, List<JobTracker>> logs = new HashMap<Class<? extends RemoteAction>, List<JobTracker>>();

	@Override
	public void getLogsForAction(final RemoteAction action, int count,
			final AsyncCallback<List<JobTracker>> outerCallback,
			boolean refresh) {
		if (logs.get(action.getClass()) == null || refresh) {
			AsyncCallback<List<JobTracker>> callback = new AsyncCallback<List<JobTracker>>() {
				@Override
				public void onFailure(Throwable caught) {
					throw new WrappedRuntimeException(caught);
				}

				@Override
				public void onSuccess(List<JobTracker> result) {
					logs.put(action.getClass(), result);
					outerCallback.onSuccess(logs.get(action.getClass()));
				}
			};
			Client.commonRemoteService().getLogsForAction(action, count,
					callback);
		} else {
			outerCallback.onSuccess(logs.get(action.getClass()));
		}
	}
}

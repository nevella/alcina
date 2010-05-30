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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.ActionLogProvider;
import cc.alcina.framework.common.client.actions.RemoteAction;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 *
 * @author Nick Reddel
 */

 public class StandardActionLogProvider implements ActionLogProvider {
	private Map<Class<? extends RemoteAction>, List<ActionLogItem>> logs = new HashMap<Class<? extends RemoteAction>, List<ActionLogItem>>();

	public void getLogsForAction(
			final RemoteAction action, int count,
			final AsyncCallback<List<ActionLogItem>> outerCallback,
			boolean refresh) {
		if (logs.get(action.getClass()) == null || refresh) {
			AsyncCallback<List<ActionLogItem>> callback = new AsyncCallback<List<ActionLogItem>>() {
				public void onFailure(Throwable caught) {
					ClientLayerLocator.get().notifications().showError(caught);
				}

				public void onSuccess(List<ActionLogItem> result) {
					logs.put(action.getClass(), result);
					outerCallback.onSuccess(logs.get(action.getClass()));
				}
			};
			ClientLayerLocator.get().commonRemoteServiceAsync().getLogsForAction(
					action, count, callback);
		} else {
			outerCallback.onSuccess(logs.get(action.getClass()));
		}
	}

	public void insertLogForAction(
			RemoteAction action, ActionLogItem item) {
		if (logs.get(action.getClass()) == null) {
			logs.put(action.getClass(), new ArrayList<ActionLogItem>());
		}
		logs.get(action.getClass()).add(0, item);
	}
}

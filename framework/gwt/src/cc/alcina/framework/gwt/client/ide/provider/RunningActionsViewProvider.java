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
package cc.alcina.framework.gwt.client.ide.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ide.widget.ActionProgress;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Nick Reddel
 */
public class RunningActionsViewProvider implements ViewProvider {
	private FlowPanel wrapper;

	private FlowPanel actionViewPanel;

	private Map<String, ActionProgress> progressPanels = new HashMap<String, ActionProgress>();

	private Timer refreshTimer = new Timer() {
		@Override
		public void run() {
			refreshActions();
		}
	};

	public Widget getViewForObject(Object obj) {
		progressPanels.clear();
		wrapper = new FlowPanel() {
			@Override
			protected void onDetach() {
				refreshTimer.cancel();
				super.onDetach();
			}
		};
		wrapper.setStyleName("alcina-BeanPanel");
		wrapper.ensureDebugId(AlcinaDebugIds.MISC_ALCINA_BEAN_PANEL);
		wrapper.add(createCaption());
		this.actionViewPanel = new FlowPanel();
		wrapper.add(actionViewPanel);
		refreshActions();
		return wrapper;
	}

	protected void refreshActions() {
		AsyncCallback<List<String>> callback = new AsyncCallback<List<String>>() {
			public void onFailure(Throwable caught) {
				// ignore
			}

			public void onSuccess(List<String> actionIds) {
				for (String id : actionIds) {
					if (!progressPanels.containsKey(id)) {
						ActionProgress actionProgress = new ActionProgress(id);
						progressPanels.put(id, actionProgress);
						actionViewPanel.add(actionProgress);
					} else {
						progressPanels.get(id).ensureRunning();
					}
				}
				for (String id : new ArrayList<String>(progressPanels.keySet())) {
					if (!actionIds.contains(id)) {
						actionViewPanel.remove(progressPanels.get(id));
						progressPanels.remove(id);
					}
				}
				refreshTimer.schedule(10000);
			}
		};
		ClientBase.getCommonRemoteServiceAsyncInstance().listRunningJobs(
				callback);
	}

	private Widget createCaption() {
		List<SimpleHistoryEventInfo> history = Arrays
				.asList(new SimpleHistoryEventInfo[] { new SimpleHistoryEventInfo(
						"Running actions") });
		return new BreadcrumbBar(null, history,
				BreadcrumbBar.maxButton(wrapper));
	}

	public static class ShowActionsViewProviderAction extends PermissibleAction {
		@Override
		public String getDisplayName() {
			return "Show running jobs";
		}
	}
}

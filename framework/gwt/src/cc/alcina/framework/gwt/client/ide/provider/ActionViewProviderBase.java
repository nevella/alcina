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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.ActionLogProvider;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.RemoteActionWithParameters;
import cc.alcina.framework.common.client.actions.SynchronousAction;
import cc.alcina.framework.common.client.actions.instances.ViewAction;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceExtAsync;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.gwittir.GwittirUtils;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.ide.widget.ActionProgress;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.SimpleHistoryEventInfo;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar.BreadcrumbBarMaximiseButton;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.handlers.HasChildHandlersSupport;

/**
 *
 * @author Nick Reddel
 */
public abstract class ActionViewProviderBase implements ViewProvider,
		PermissibleActionListener {
	private PaneWrapper wrapper;

	private BreadcrumbBarMaximiseButton maxButton;

	protected RemoteAction action;

	protected boolean alwaysExpandFirst() {
		return false;
	}

	public Widget getViewForObject(Object obj) {
		action = (RemoteAction) obj;
		wrapper = new PaneWrapper();
		wrapper.setStyleName("alcina-BeanPanel");
		wrapper.ensureDebugId(AlcinaDebugIds.MISC_ALCINA_BEAN_PANEL);
		wrapper.addVetoableActionListener(this);
		wrapper.add(createCaption(action));
		wrapper.add(new ActionLogPanel());
		wrapper.setAction(action);
		return wrapper;
	}

	public void vetoableAction(PermissibleActionEvent evt) {
		// no response at the mo'
	}

	private Widget createCaption(PermissibleAction action) {
		List<SimpleHistoryEventInfo> history = Arrays
				.asList(new SimpleHistoryEventInfo[] {
						new SimpleHistoryEventInfo("Action"),
						new SimpleHistoryEventInfo(action.getDisplayName()) });
		List<Widget> maxButtonArr = BreadcrumbBar.maxButton(wrapper);
		this.maxButton = (BreadcrumbBarMaximiseButton) maxButtonArr.get(0);
		return new BreadcrumbBar(null, history, maxButtonArr);
	}

	protected void getActionLogs(
			AsyncCallback<List<ActionLogItem>> outerCallback, int logItemCount) {
		Registry.impl(ActionLogProvider.class).getLogsForAction(action,
				logItemCount, outerCallback, true);
	}

	protected abstract void performAction(AsyncCallback<String> asyncCallback,
			AsyncCallback<ActionLogItem> syncCallback);

	public static class ActionViewProvider extends ActionViewProviderBase {
		protected void performAction(AsyncCallback<String> asyncCallback,
				AsyncCallback<ActionLogItem> syncCallback) {
			if (action instanceof SynchronousAction) {
				((CommonRemoteServiceExtAsync) ClientBase
						.getCommonRemoteServiceAsyncInstance())
						.performActionAndWait(action, syncCallback);
			} else {
				((CommonRemoteServiceExtAsync) ClientBase
						.getCommonRemoteServiceAsyncInstance()).performAction(
						action, asyncCallback);
			}
		}
	}

	public class ActionLogPanel extends VerticalPanel implements ClickHandler {
		private static final String RUNNING = "...running";

		private Button button;

		private FlowPanel fp;

		private Label runningLabel;

		private PaneWrapperWithObjects beanView;

		private ActionProgress actionProgress;

		private HasChildHandlersSupport hasChildHandlersSupport;

		private FlowPanel progressHolder;

		private PropertyChangeListener progressPcl = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (actionProgress.getJobInfo().isComplete()) {
					redraw();
				}
			}
		};

		private LooseActionHandler handler;

		int logItemCount = 5;

		public ActionLogPanel() {
			this.handler = LooseActionRegistry.get().getHandler(
					action.getClass().getName());
			this.hasChildHandlersSupport = new HasChildHandlersSupport();
			HTML description = new HTML("<i>" + action.getDescription()
					+ "</i><br />");
			this.fp = new FlowPanel();
			add(description);
			if (action instanceof RemoteActionWithParameters) {
				ContentViewFactory cvf = new ContentViewFactory();
				cvf.setNoCaptionsOrButtons(true);
				this.beanView = cvf.createBeanView(
						((RemoteActionWithParameters) action).getParameters(),
						true, null, false, true);
				add(beanView);
			}
			this.button = new Button("Run now");
			button.ensureDebugId(AlcinaDebugIds.ACTION_VIEW_RUN);
			// dill, not redrawn
			// hasChildHandlersSupport.addHandler(button.addClickHandler(this));
			button.addClickHandler(this);
			HorizontalPanel hp = new HorizontalPanel();
			hp.add(button);
			hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
			this.runningLabel = new Label(RUNNING);
			hp.add(runningLabel);
			add(hp);
			this.progressHolder = new FlowPanel();
			progressHolder.setVisible(false);
			add(progressHolder);
			if (handler == null) {
				add(new HTML(
						CommonUtils
								.formatJ(
										"<br /><hr /><div class='recent'>Last %s action logs</div><br />",
										logItemCount)));
			}
			add(fp);
			setWidth("80%");
			redraw();
		}

		public void addHandler(HandlerRegistration registration) {
			this.hasChildHandlersSupport.addHandler(registration);
		}

		public void detachHandlers() {
			this.hasChildHandlersSupport.detachHandlers();
		}

		public boolean equals(Object obj) {
			return this.hasChildHandlersSupport.equals(obj);
		}

		public int hashCode() {
			return this.hasChildHandlersSupport.hashCode();
		}

		public void onClick(ClickEvent event) {
			boolean running = runningLabel.isVisible();
			runningLabel.setText(RUNNING);
			AsyncCallback<String> asyncCallback = new AsyncCallback<String>() {
				public void onFailure(Throwable caught) {
					redraw();
					throw new WrappedRuntimeException(caught);
				}

				public void onSuccess(String id) {
					if (actionProgress != null) {
						actionProgress
								.removePropertyChangeListener(progressPcl);
						progressHolder.clear();
					}
					if (id != null) {
						actionProgress = new ActionProgress(id);
						actionProgress.addPropertyChangeListener(progressPcl);
						progressHolder.add(actionProgress);
					} else {
						Label w = new Label("...job queued");
						w.setStyleName("pad-15");
						progressHolder.add(w);
					}
					progressHolder.setVisible(true);
					redraw();
					running(id != null);
				}
			};
			AsyncCallback<ActionLogItem> syncCallback = new AsyncCallback<ActionLogItem>() {
				public void onFailure(Throwable caught) {
					redraw();
					throw new WrappedRuntimeException(caught);
				}

				public void onSuccess(ActionLogItem ali) {
					if (handler != null) {
						// actually a combined client-server action
						handler.performAction();
						return;
					}
				}
			};
			GwittirUtils.refreshEmptyTextBoxes(beanView.getBoundWidget()
					.getBinding());
			if (!beanView.getBoundWidget().getBinding().validate()) {
				Registry.impl(ClientNotifications.class).showWarning(
						"Please correct the problems in the form");
				return;
			}
			running(true);
			performAction(asyncCallback, syncCallback);
		}

		public String toString() {
			return this.hasChildHandlersSupport.toString();
		}

		private void running(boolean running) {
			button.setText("Run now");
			button.setEnabled(!running);
			runningLabel.setVisible(running);
		}

		protected void redraw() {
			WidgetUtils.clearChildren(fp);
			hasChildHandlersSupport.detachHandlers();
			AsyncCallback<List<ActionLogItem>> outerCallback = new AsyncCallback<List<ActionLogItem>>() {
				public void onFailure(Throwable caught) {
					throw new WrappedRuntimeException(caught);
				}

				public void onSuccess(List<ActionLogItem> result) {
					int idx = 0;
					for (ActionLogItem actionLogItem : result) {
						fp.add(new ActionLogItemVisualiser(actionLogItem,
								idx++ == 0));
					}
					HorizontalPanel more = new HorizontalPanel();
					more.setStyleName("pad-15 action-logs");
					more.setSpacing(2);
					more.add(new InlineLabel("Show more - "));
					int[] counts = { 10, 20, 40, 80, 160, 320 };
					for (int c : counts) {
						final int fc = c;
						more.add(new Link(String.valueOf(c),
								new ClickHandler() {
									public void onClick(ClickEvent event) {
										logItemCount = fc;
										redraw();
									}
								}));
						more.add(new InlineHTML("&nbsp;"));
					}
					fp.add(more);
				}
			};
			if (beanView != null) {
				beanView.setVisible(true);
			}
			running(false);
			button.setEnabled(true);
			if (handler == null) {
				getActionLogs(outerCallback, logItemCount);
			}
			maxButton.getToggleButton().setDown(true);
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					maxButton.getToggleButton().setDown(false);
				}
			});
		}
	}

	class ActionLogItemVisualiser extends Composite implements ClickHandler {
		private Link link;

		private HTML html;

		private VerticalPanel vp;

		ActionLogItemVisualiser(ActionLogItem item, boolean first) {
			this.vp = new VerticalPanel();
			this.link = new Link(CommonUtils.formatDate(item.getActionDate(),
					DateStyle.AU_DATE_TIME)
					+ " - "
					+ item.getShortDescription());
			link.addClickHandler(this);
			String actionLog = item.getActionLog();
			if(actionLog==null){
				actionLog="{no log}";
			}
			boolean customHtml = actionLog.contains("div");
			this.html = new HTML(customHtml ? actionLog : "<pre>" + actionLog
					+ "</pre>", true);
			html.setVisible(first
					&& (actionLog.length() < 2000 || alwaysExpandFirst()));
			if (!customHtml) {
				html.setStyleName("logboxpre");
			}
			vp.add(link);
			vp.add(html);
			initWidget(vp);
		}

		public void onClick(ClickEvent event) {
			html.setVisible(!html.isVisible());
		}
	}

	static class PaneWrapper extends FlowPanel implements ClickHandler,
			PermissibleActionEvent.PermissibleActionSource {
		private PermissibleAction action;

		PermissibleActionEvent.PermissibleActionSupport support = new PermissibleActionEvent.PermissibleActionSupport();

		@SuppressWarnings("unused")
		private Button saveButton;

		public void addVetoableActionListener(PermissibleActionListener listener) {
			this.support.addVetoableActionListener(listener);
		}

		public void fireVetoableActionEvent(PermissibleActionEvent event) {
			this.support.fireVetoableActionEvent(event);
		}

		public PermissibleAction getAction() {
			return action;
		}

		public void onClick(ClickEvent event) {
			PermissibleActionEvent action = new PermissibleActionEvent(
					this.action, ClientReflector.get().newInstance(
							ViewAction.class));
			fireVetoableActionEvent(action);
		}

		public void removeVetoableActionListener(
				PermissibleActionListener listener) {
			this.support.removeVetoableActionListener(listener);
		}

		public void setAction(PermissibleAction action) {
			this.action = action;
		}

		public void setSaveButton(Button saveButton) {
			this.saveButton = saveButton;
		}
	}
}

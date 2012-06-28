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
package cc.alcina.framework.gwt.client.ide.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.actions.ActionGroup;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.StyledAWidget;
import cc.alcina.framework.gwt.client.widget.handlers.HasChildHandlers;
import cc.alcina.framework.gwt.client.widget.handlers.HasChildHandlersSupport;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Nick Reddel
 */
public class Toolbar extends Composite implements
		PermissibleActionEvent.PermissibleActionSource, ClickHandler,
		HasChildHandlers {
	public void addHandler(HandlerRegistration registration) {
		this.hasChildHandlersSupport.addHandler(registration);
	}

	public void detachHandlers() {
		this.hasChildHandlersSupport.detachHandlers();
	}

	private List<PermissibleAction> actions;

	private Map<Class<? extends PermissibleAction>, ToolbarButton> actionButtons;

	private FlowPanel panel;

	public FlowPanel getPanel() {
		return this.panel;
	}

	private PermissibleActionEvent.PermissibleActionSupport vetoableActionSupport;

	private List<ActionGroup> actionGroups;

	private boolean hideUnpermittedActions;

	private boolean asButton;

	private Map<PermissibleAction, ToolbarButton> actionButtonsByAction;

	private HasChildHandlersSupport hasChildHandlersSupport;

	public Toolbar() {
		this.hasChildHandlersSupport = new HasChildHandlersSupport();
		this.panel = new FlowPanel();
		this.actions = new ArrayList<PermissibleAction>();
		panel.setStyleName("toolbar clearfix");
		this.vetoableActionSupport = new PermissibleActionEvent.PermissibleActionSupport();
		initWidget(panel);
		redraw();
	}

	public void addVetoableActionListener(PermissibleActionListener listener) {
		this.vetoableActionSupport.addVetoableActionListener(listener);
	}

	public void enableAll(boolean enable) {
		if (actionButtons == null) {
			return;
		}
		Collection<ToolbarButton> buttons = actionButtonsByAction.values();
		for (ToolbarButton toolbarButton : buttons) {
			toolbarButton.setEnabled(enable);
		}
	}

	public List<ActionGroup> getActionGroups() {
		return actionGroups;
	}

	public List<PermissibleAction> getActions() {
		return this.actions;
	}

	public ToolbarButton getButtonForAction(PermissibleAction action) {
		List<Widget> kids = WidgetUtils.allChildren(panel);
		for (Widget widget : kids) {
			if (widget instanceof ToolbarButton) {
				ToolbarButton tb = (ToolbarButton) widget;
				if (tb.getAction() == action) {
					return tb;
				}
			}
		}
		return null;
	}

	public boolean isHideUnpermittedActions() {
		return hideUnpermittedActions;
	}

	private boolean removeListenersOnDetach = true;

	@Override
	protected void onDetach() {
		super.onDetach();
		if (removeListenersOnDetach) {
			vetoableActionSupport.removeAllListeners();
		}
	}

	public void processAvailableActions(
			List<Class<? extends PermissibleAction>> avActions) {
		if (actionButtons == null) {
			return;
		}
		Collection<ToolbarButton> buttons = actionButtons.values();
		for (ToolbarButton toolbarButton : buttons) {
			toolbarButton.setEnabled(false);
		}
		for (Class<? extends PermissibleAction> actionClass : avActions) {
			ToolbarButton tb = actionButtons.get(actionClass);
			if (tb != null) {
				tb.setEnabled(true);
			}
		}
	}

	protected void redraw() {
		WidgetUtils.clearChildren(panel);
		hasChildHandlersSupport.detachHandlers();
		actionButtons = new HashMap<Class<? extends PermissibleAction>, ToolbarButton>();
		actionButtonsByAction = new HashMap<PermissibleAction, ToolbarButton>();
		boolean hasActionGroups = actionGroups != null
				&& actionGroups.size() != 0;
		if (hasActionGroups) {
			actions.removeAll(actions);
			for (ActionGroup g : actionGroups) {
				FlowPanel fp = new FlowPanel();
				fp.setStyleName("group");
				if (g.isRightAligned()) {
					fp.addStyleName("float-right");
				}
				for (PermissibleAction action : g.actions) {
					ToolbarButton button = new ToolbarButton(action, asButton);
					hasChildHandlersSupport.addHandler(button
							.addClickHandler(this));
					button.setEnabled(false);
					fp.add(button);
					actionButtons.put(action.getClass(), button);
					actionButtonsByAction.put(action, button);
					actions.add(action);
					if (isHideUnpermittedActions()
							&& action instanceof Permissible
							&& !PermissionsManager.get().isPermissible(
									(Permissible) action)) {
						button.setVisible(false);
					}
				}
				panel.add(fp);
			}
		} else {
			for (PermissibleAction action : actions) {
				ToolbarButton button = new ToolbarButton(action, asButton);
				HandlerRegistration registration = button.addClickHandler(this);
				hasChildHandlersSupport.addHandler(registration);
				button.setEnabled(false);
				panel.add(button);
				actionButtons.put(action.getClass(), button);
				actionButtonsByAction.put(action, button);
				if (isHideUnpermittedActions()
						&& action instanceof Permissible
						&& !PermissionsManager.get().isPermissible(
								(Permissible) action)) {
					button.setVisible(false);
				}
			}
		}
	}

	public void removeVetoableActionListener(PermissibleActionListener listener) {
		this.vetoableActionSupport.removeVetoableActionListener(listener);
	}

	public void setActionGroups(List<ActionGroup> actionGroups) {
		this.actionGroups = actionGroups;
		redraw();
	}

	public void setActions(List<PermissibleAction> actions) {
		this.actions = actions;
		redraw();
	}

	public void setHideUnpermittedActions(boolean hideUnpermittedActions) {
		this.hideUnpermittedActions = hideUnpermittedActions;
	}

	public void setAsButton(boolean asButton) {
		this.asButton = asButton;
	}

	public boolean isAsButton() {
		return asButton;
	}

	public void setRemoveListenersOnDetach(boolean removeListenersOnDetach) {
		this.removeListenersOnDetach = removeListenersOnDetach;
	}

	public boolean isRemoveListenersOnDetach() {
		return removeListenersOnDetach;
	}

	public static class ToolbarButton extends Composite implements
			HasClickHandlers {
		private final PermissibleAction action;

		private StyledAWidget aWidget;

		private Button button;

		private final boolean asButton;

		public ToolbarButton(PermissibleAction action) {
			this(action, false);
		}

		public ToolbarButton(PermissibleAction action, boolean asButton) {
			this.asButton = asButton;
			Widget w = null;
			if (asButton) {
				button = new Button(action.getDisplayName());
				button.setStyleName("alcina-Button");
				w = button;
				if (action instanceof ClickHandler) {
					button.addClickHandler((ClickHandler) action);
				}
			} else {
				aWidget = new StyledAWidget(action.getDisplayName(), true);
				aWidget.setStyleName("button-grey");
				aWidget.setWordWrap(false);
				w = aWidget;
				if (action instanceof ClickHandler) {
					aWidget.addClickHandler((ClickHandler) action);
				}
			}
			String cn = action.getCssClassName();
			if (cn != null) {
				w.addStyleName(cn);
			}
			this.action = action;
			initWidget(w);
		}

		public String getTarget() {
			return this.aWidget.getTarget();
		}

		public void setTarget(String target) {
			this.aWidget.setTarget(target);
		}

		public String getHref() {
			return this.aWidget.getHref();
		}

		public void setHref(String href) {
			this.aWidget.setHref(href);
		}

		public void setEnabled(boolean enabled) {
			if (asButton) {
				button.setEnabled(enabled);
			} else {
				aWidget.setEnabled(enabled);
			}
		}

		public void setText(String text) {
			if (asButton) {
				button.setText(text);
			} else {
				aWidget.setText(text);
			}
		}

		public PermissibleAction getAction() {
			return this.action;
		}

		public HandlerRegistration addClickHandler(ClickHandler handler) {
			if (asButton) {
				return button.addClickHandler(handler);
			} else {
				return aWidget.addClickHandler(handler);
			}
		}
	}

	public void onClick(ClickEvent event) {
		Widget sender = (Widget) event.getSource();
		if (sender.getParent() instanceof ToolbarButton) {
			ToolbarButton tb = (ToolbarButton) sender.getParent();
			vetoableActionSupport
					.fireVetoableActionEvent(new PermissibleActionEvent(sender,
							tb.getAction()));
		}
	}
}

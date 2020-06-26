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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.ActionGroup;
import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleAction.HasPermissibleActionChildren;
import cc.alcina.framework.common.client.actions.PermissibleAction.HasPermissibleActionDelegate;
import cc.alcina.framework.common.client.actions.PermissibleAction.PermissibleActionWithDelegate;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.logic.HasHref;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImageProvider;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupAxis;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.SpanPanel;
import cc.alcina.framework.gwt.client.widget.StyledAWidget;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;
import cc.alcina.framework.gwt.client.widget.handlers.HasChildHandlers;
import cc.alcina.framework.gwt.client.widget.handlers.HasChildHandlersSupport;

/**
 * 
 * @author Nick Reddel
 */
public class Toolbar extends Composite
		implements PermissibleActionEvent.PermissibleActionSource, ClickHandler,
		HasChildHandlers {
	public static final String CONTEXT_DO_NOT_REMOVE_LISTENERS_ON_DETACH = Toolbar.class
			.getName() + ".CONTEXT_DO_NOT_REMOVE_LISTENERS_ON_DETACH";

	private List<PermissibleAction> actions;

	private Map<Class<? extends PermissibleAction>, ToolbarButton> actionButtons;

	private FlowPanel panel;

	private PermissibleActionEvent.PermissibleActionSupport vetoableActionSupport;

	private List<ActionGroup> actionGroups;

	private boolean hideUnpermittedActions;

	private boolean asButton;

	private Map<PermissibleAction, ToolbarButton> actionButtonsByAction;

	private HasChildHandlersSupport hasChildHandlersSupport;

	private boolean removeListenersOnDetach = true;

	private String buttonStyleName;

	public Toolbar() {
		this.hasChildHandlersSupport = new HasChildHandlersSupport();
		this.panel = new FlowPanel();
		this.actions = new ArrayList<PermissibleAction>();
		panel.setStyleName("toolbar clearfix");
		this.vetoableActionSupport = new PermissibleActionEvent.PermissibleActionSupport();
		initWidget(panel);
		getElement().setId(Document.get().createUniqueId());
		redraw();
	}

	@Override
	public void addHandler(HandlerRegistration registration) {
		this.hasChildHandlersSupport.addHandler(registration);
	}

	@Override
	public void addVetoableActionListener(PermissibleActionListener listener) {
		this.vetoableActionSupport.addVetoableActionListener(listener);
	}

	@Override
	public void detachHandlers() {
		this.hasChildHandlersSupport.detachHandlers();
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

	public String getButtonStyleName() {
		return this.buttonStyleName;
	}

	public FlowPanel getPanel() {
		return this.panel;
	}

	public boolean isAsButton() {
		return asButton;
	}

	public boolean isHideUnpermittedActions() {
		return hideUnpermittedActions;
	}

	public boolean isRemoveListenersOnDetach() {
		return removeListenersOnDetach;
	}

	@Override
	public void onClick(ClickEvent event) {
		Widget sender = (Widget) event.getSource();
		if (sender.getParent() instanceof ToolbarButton) {
			ToolbarButton tb = (ToolbarButton) sender.getParent();
			vetoableActionSupport.fireVetoableActionEvent(
					new PermissibleActionEvent(sender, tb.getAction()));
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

	@Override
	public void
			removeVetoableActionListener(PermissibleActionListener listener) {
		this.vetoableActionSupport.removeVetoableActionListener(listener);
	}

	public void setActionGroup(ActionGroup actionGroup) {
		setActionGroups(Collections.singletonList(actionGroup));
	}

	public void setActionGroups(List<ActionGroup> actionGroups) {
		this.actionGroups = actionGroups;
		redraw();
	}

	public void setActions(List<PermissibleAction> actions) {
		this.actions = actions;
		redraw();
	}

	public void setAsButton(boolean asButton) {
		this.asButton = asButton;
	}

	public void setButtonStyleName(String buttonStyleName) {
		this.buttonStyleName = buttonStyleName;
	}

	public void setHideUnpermittedActions(boolean hideUnpermittedActions) {
		this.hideUnpermittedActions = hideUnpermittedActions;
	}

	public void setRemoveListenersOnDetach(boolean removeListenersOnDetach) {
		this.removeListenersOnDetach = removeListenersOnDetach;
	}

	@Override
	protected void onDetach() {
		super.onDetach();
		if (removeListenersOnDetach && !LooseContext
				.getBoolean(CONTEXT_DO_NOT_REMOVE_LISTENERS_ON_DETACH)) {
			vetoableActionSupport.removeAllListeners();
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
				if (g.actions.isEmpty()) {
					continue;
				}
				FlowPanel fp = new FlowPanel();
				fp.setStyleName("group");
				if (g.isRightAligned()) {
					fp.addStyleName("float-right");
				}
				for (PermissibleAction action : g.actions) {
					ToolbarButton button = new ToolbarButton(action,
							buttonStyleName, asButton);
					hasChildHandlersSupport
							.addHandler(button.addClickHandler(this));
					fp.add(button);
					actionButtons.put(action.getClass(), button);
					actionButtonsByAction.put(action, button);
					actions.add(action);
					if (isHideUnpermittedActions()
							&& action instanceof Permissible
							&& !PermissionsManager.get()
									.isPermissible((Permissible) action)) {
						button.setVisible(false);
					}
				}
				panel.add(fp);
			}
		} else {
			for (PermissibleAction action : actions) {
				ToolbarButton button = new ToolbarButton(action,
						buttonStyleName, asButton);
				HandlerRegistration registration = button.addClickHandler(this);
				hasChildHandlersSupport.addHandler(registration);
				panel.add(button);
				actionButtons.put(action.getClass(), button);
				actionButtonsByAction.put(action, button);
				if (isHideUnpermittedActions() && action instanceof Permissible
						&& !PermissionsManager.get()
								.isPermissible((Permissible) action)) {
					button.setVisible(false);
				}
			}
		}
	}

	public static interface HasDropdownPresenter {
		public Widget presentDropdown();

		public void setPopup(RelativePopupPanel rpp);
	}

	public abstract static class PermissibleActionWithDelegateAndDropdown
			extends PermissibleActionWithDelegate
			implements HasDropdownPresenter {
		public PermissibleActionWithDelegateAndDropdown(
				PermissibleAction delegate) {
			super(delegate);
		}
	}

	public static class ToolbarButton extends Composite
			implements HasClickHandlers {
		private final PermissibleAction action;

		private StyledAWidget aWidget;

		private Button button;

		private final boolean asButton;

		private StyledAWidget dropDown;

		public ToolbarButton(PermissibleAction action) {
			this(action, false);
		}

		public ToolbarButton(PermissibleAction action, boolean asButton) {
			this(action, null, asButton);
		}

		public ToolbarButton(PermissibleAction action, String buttonStyleName,
				boolean asButton) {
			this.asButton = asButton;
			this.action = action;
			Widget w = null;
			String cn = action.getCssClassName();
			if (asButton) {
				button = new Button(SafeHtmlUtils
						.htmlEscapeAllowEntities(action.getDisplayName()));
				button.setStyleName(buttonStyleName != null ? buttonStyleName
						: "alcina-Button");
				w = button;
				if (action instanceof ClickHandler) {
					button.addClickHandler((ClickHandler) action);
				}
			} else {
				aWidget = new StyledAWidget(action.getDisplayName(), false);
				aWidget.setStyleName(buttonStyleName != null ? buttonStyleName
						: "button-grey");
				aWidget.setWordWrap(false);
				w = aWidget;
				if (action instanceof ClickHandler) {
					aWidget.addClickHandler((ClickHandler) action);
				}
				refreshHref();
				if (action instanceof PermissibleActionWithDelegate) {
					PermissibleAction delegate = ((HasPermissibleActionDelegate) action)
							.getDelegate();
					if (delegate instanceof ClickHandler) {
						aWidget.addClickHandler((ClickHandler) delegate);
					}
				}
				if (action instanceof HasPermissibleActionChildren
						|| action instanceof HasDropdownPresenter) {
					SpanPanel sp = new SpanPanel();
					w = sp;
					sp.add(aWidget);
					if (cn != null) {
						aWidget.addStyleName(cn);
					}
					aWidget.addStyleName("pre-drop-down");
					AbstractImagePrototype aip = AbstractImagePrototype
							.create(StandardDataImageProvider.get()
									.getDataImages().downGrey());
					dropDown = new StyledAWidget(aip.getHTML(), true);
					dropDown.addStyleName("button-grey drop-down");
					sp.add(dropDown);
					dropDown.addClickHandler(new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							showDropDown();
						}
					});
				}
			}
			if (cn != null) {
				w.addStyleName(cn);
			}
			initWidget(w);
		}

		@Override
		public HandlerRegistration addClickHandler(ClickHandler handler) {
			if (asButton) {
				return button.addClickHandler(handler);
			} else {
				return aWidget.addClickHandler(handler);
			}
		}

		public PermissibleAction getAction() {
			return this.action;
		}

		public String getHref() {
			return this.aWidget.getHref();
		}

		public String getTarget() {
			return this.aWidget.getTarget();
		}

		public void refreshHref() {
			if (action instanceof HasHref) {
				setHref(((HasHref) action).getHref());
				setTarget(((HasHref) action).getTarget());
			}
		}

		public void setEnabled(boolean enabled) {
			if (asButton) {
				button.setEnabled(enabled);
			} else {
				aWidget.setEnabled(enabled);
			}
		}

		public void setHref(String href) {
			if (CommonUtils.isNotNullOrEmpty(href)) {
				this.aWidget.setHref(href);
				this.aWidget.setPreventDefault(false);
			}
		}

		public void setTarget(String target) {
			this.aWidget.setTarget(target);
		}

		public void setText(String text) {
			if (asButton) {
				button.setText(text);
			} else {
				aWidget.setText(text);
			}
		}

		public void setWordWrap(boolean wordWrap) {
			if (asButton) {
			} else {
				aWidget.setWordWrap(wordWrap);
			}
		}

		public ToolbarButton withCssClassName(String className) {
			aWidget.setStyleName(className);
			return this;
		}

		protected void showDropDown() {
			Widget dropDown = null;
			if (action instanceof HasDropdownPresenter) {
				dropDown = ((HasDropdownPresenter) action).presentDropdown();
			} else {
				VerticalPanel vp = new VerticalPanel();
				dropDown = vp;
				HasPermissibleActionChildren wKids = (HasPermissibleActionChildren) action;
				for (PermissibleAction a : wKids.getChildren()) {
					vp.add(new ToolbarButton(a));
				}
			}
			RelativePopupPanel rpp = new RelativePopupPanel(true);
			rpp.setAnimationEnabled(false);
			RelativePopupPositioning.showPopup(getWidget(), dropDown,
					RootPanel.get(),
					new RelativePopupAxis[] {
							RelativePopupPositioning.BOTTOM_LTR },
					RootPanel.get(), rpp, -6, 6);
			rpp.setAnimationEnabled(false);
			if (action instanceof HasDropdownPresenter) {
				((HasDropdownPresenter) action).setPopup(rpp);
			} else {
				rpp.addStyleName("child-actions");
			}
			rpp.addStyleName("toolbar-button-dropdown");
		}
	}
}

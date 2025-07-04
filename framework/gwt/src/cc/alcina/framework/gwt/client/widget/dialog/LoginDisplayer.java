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
package cc.alcina.framework.gwt.client.widget.dialog;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.CancelAction;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.EnterAsClickKeyboardListener;

/**
 *
 * @author Nick Reddel
 */
public class LoginDisplayer {
	private DialogBox dialogBox;

	private Label statusLabel;

	private Button cancelButton;

	private Button okButton;

	private FlexTable table;

	private Widget onProblemWidget = new Label("blank");

	private FlowPanel introWidget;

	private Label usernameLabel;

	private CheckBox rememberMeBox;

	private TextBox nameBox;

	private PasswordTextBox pwdBox;

	private FlowPanel mainPanel;

	public LoginDisplayer() {
		dialogBox = new GlassDialogBox();
		dialogBox.setText("Login");
		dialogBox.setAnimationEnabled(true);
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("alcina-Login");
		mainPanel.ensureDebugId(AlcinaDebugIds.LOGIN_FORM);
		this.introWidget = new FlowPanel();
		introWidget.setVisible(false);
		mainPanel.add(introWidget);
		introWidget.setStyleName("intro");
		cancelButton = new Button("Cancel");
		okButton = new Button("Login");
		okButton.ensureDebugId(AlcinaDebugIds.LOGIN_SUBMIT);
		table = new FlexTable();
		table.setWidth("100%");
		table.setCellSpacing(2);
		this.usernameLabel = new Label("Username: ");
		table.setWidget(0, 0, usernameLabel);
		nameBox = new TextBox();
		WidgetUtils.disableTextBoxHelpers(nameBox);
		nameBox.ensureDebugId(AlcinaDebugIds.LOGIN_USERNAME);
		table.setWidget(0, 1, nameBox);
		table.setWidget(1, 0, new Label("Password: "));
		pwdBox = new PasswordTextBox();
		WidgetUtils.disableTextBoxHelpers(pwdBox);
		pwdBox.ensureDebugId(AlcinaDebugIds.LOGIN_PASSWORD);
		table.setWidget(1, 1, pwdBox);
		pwdBox.addKeyPressHandler(
				new EnterAsClickKeyboardListener(pwdBox, okButton));
		nameBox.addKeyPressHandler(
				new EnterAsClickKeyboardListener(nameBox, okButton));
		rememberMeBox = new CheckBox();
		rememberMeBox.setValue(true);
		table.setWidget(2, 0, rememberMeBox);
		table.getCellFormatter().setHorizontalAlignment(2, 0,
				HasHorizontalAlignment.ALIGN_RIGHT);
		table.getCellFormatter().setHorizontalAlignment(1, 0,
				HasHorizontalAlignment.ALIGN_RIGHT);
		// hard-code for approximately centered <input> elements
		table.getCellFormatter().getElement(1, 0).getStyle().setPaddingLeft(12,
				Unit.PX);
		table.getCellFormatter().setHorizontalAlignment(0, 0,
				HasHorizontalAlignment.ALIGN_RIGHT);
		table.setWidget(2, 1, new Label("Remember me on this computer"));
		statusLabel = new Label("Logging in");
		statusLabel.setVisible(false);
		table.setWidget(4, 1, statusLabel);
		HorizontalPanel hPanel = new HorizontalPanel();
		hPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
		hPanel.setSpacing(5);
		hPanel.add(okButton);
		okButton.addStyleName("marginRight10");
		hPanel.add(cancelButton);
		table.setWidget(3, 0, hPanel);
		table.getFlexCellFormatter().setColSpan(3, 0, 2);
		table.getCellFormatter().getElement(3, 0).setAttribute("align",
				"center");
		mainPanel.add(table);
		dialogBox.setWidget(mainPanel);
	}

	public void addAlternateAuthWidget(Widget alternateAuthWidget) {
		table.setWidget(6, 0, alternateAuthWidget);
		table.getFlexCellFormatter().setHorizontalAlignment(6, 0,
				HasHorizontalAlignment.ALIGN_CENTER);
		table.getFlexCellFormatter().setColSpan(6, 0, 2);
	}

	public void enableLoginButtons(boolean enable) {
		okButton.setEnabled(enable);
		cancelButton.setEnabled(enable);
	}

	public DialogBox getDialogBox() {
		return this.dialogBox;
	}

	public FlowPanel getIntroWidget() {
		return this.introWidget;
	}

	public FlowPanel getMainPanel() {
		return this.mainPanel;
	}

	public TextBox getNameBox() {
		return this.nameBox;
	}

	public Label getStatusLabel() {
		return this.statusLabel;
	}

	public Label getUsernameLabel() {
		return this.usernameLabel;
	}

	public void hideLoginDialog() {
		dialogBox.hide();
	}

	public void setProblemHandlerWidget(Widget onProblemWidget) {
		this.onProblemWidget = onProblemWidget;
		table.setWidget(5, 0, onProblemWidget);
		table.getFlexCellFormatter().setHorizontalAlignment(5, 0,
				HasHorizontalAlignment.ALIGN_CENTER);
		table.getFlexCellFormatter().setColSpan(5, 0, 2);
	}

	public void setStatus(String s) {
		statusLabel.setVisible(true);
		statusLabel.setText(s);
	}

	public void showLoginDialog(final PermissibleActionListener listener) {
		okButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				statusLabel.setVisible(true);
				statusLabel.setStyleName("");
				onProblemWidget.setVisible(false);
				LoginBean b = new LoginBean();
				b.setUserName(nameBox.getText());
				b.setPassword(pwdBox.getText());
				b.setRememberMe(rememberMeBox.getValue());
				PermissibleAction action = new PermissibleAction.OkAction();
				PermissibleActionEvent evt = new PermissibleActionEvent(this,
						action);
				evt.setParameters(b);
				listener.vetoableAction(evt);
			}
		});
		cancelButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				PermissibleAction action = new CancelAction();
				listener.vetoableAction(
						new PermissibleActionEvent(this, action));
			}
		});
		// Set the contents of the Widget
		dialogBox.center();
		dialogBox.show();
		nameBox.setFocus(true);
	}
}

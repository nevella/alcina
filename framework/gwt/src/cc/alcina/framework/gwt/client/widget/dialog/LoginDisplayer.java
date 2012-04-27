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

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.widget.EnterAsClickKeyboardListener;

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

/**
 * 
 * @author Nick Reddel
 */
public class LoginDisplayer {
	public static final String CANCEL_ACTION = "cancel";

	public static final String LOGIN_ACTION = "ok";

	private DialogBox dialogBox;

	public DialogBox getDialogBox() {
		return this.dialogBox;
	}

	private Label statusLabel;

	public Label getStatusLabel() {
		return this.statusLabel;
	}

	private Button cancelButton;

	private Button okButton;

	private FlexTable table;

	private Widget onProblemWidget = new Label("blank");

	private FlowPanel introWidget;

	public FlowPanel getIntroWidget() {
		return this.introWidget;
	}

	private Label usernameLabel;

	private CheckBox rememberMeBox;

	private TextBox nameBox;

	public TextBox getNameBox() {
		return this.nameBox;
	}

	private PasswordTextBox pwdBox;

	private Widget alternateAuthWidget;

	public Label getUsernameLabel() {
		return this.usernameLabel;
	}

	public LoginDisplayer() {
		dialogBox = new GlassDialogBox();
		dialogBox.setText("Login");
		dialogBox.setAnimationEnabled(true);
		FlowPanel fp = new FlowPanel();
		fp.setStyleName("alcina-Login");
		fp.ensureDebugId(AlcinaDebugIds.LOGIN_FORM);
		this.introWidget = new FlowPanel();
		introWidget.setVisible(false);
		fp.add(introWidget);
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
		nameBox.ensureDebugId(AlcinaDebugIds.LOGIN_USERNAME);
		table.setWidget(0, 1, nameBox);
		table.setWidget(1, 0, new Label("Password: "));
		pwdBox = new PasswordTextBox();
		pwdBox.ensureDebugId(AlcinaDebugIds.LOGIN_PASSWORD);
		table.setWidget(1, 1, pwdBox);
		pwdBox.addKeyPressHandler(new EnterAsClickKeyboardListener(pwdBox,
				okButton));
		rememberMeBox = new CheckBox();
		rememberMeBox.setValue(true);
		table.setWidget(2, 0, rememberMeBox);
		table.getCellFormatter().setHorizontalAlignment(2, 0,
				HasHorizontalAlignment.ALIGN_RIGHT);
		table.getCellFormatter().setHorizontalAlignment(1, 0,
				HasHorizontalAlignment.ALIGN_RIGHT);
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
		table.setWidget(3, 1, hPanel);
		fp.add(table);
		dialogBox.setWidget(fp);
	}

	public void showLoginDialog(final PermissibleActionListener listener) {
		okButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				statusLabel.setVisible(true);
				statusLabel.setStyleName("");
				onProblemWidget.setVisible(false);
				LoginBean b = new LoginBean();
				b.setUserName(nameBox.getText());
				b.setPassword(pwdBox.getText());
				b.setRememberMe(rememberMeBox.getValue());
				PermissibleAction action = new PermissibleAction();
				action.setActionName(LOGIN_ACTION);
				PermissibleActionEvent evt = new PermissibleActionEvent(this,
						action);
				evt.setParameters(b);
				listener.vetoableAction(evt);
			}
		});
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				PermissibleAction action = new PermissibleAction();
				action.setActionName(CANCEL_ACTION);
				listener
						.vetoableAction(new PermissibleActionEvent(this, action));
			}
		});
		// Set the contents of the Widget
		dialogBox.center();
		dialogBox.show();
		nameBox.setFocus(true);
	}

	public void setStatus(String s) {
		statusLabel.setVisible(true);
		statusLabel.setText(s);
	}

	public void enableLoginButtons(boolean enable) {
		okButton.setEnabled(enable);
		cancelButton.setEnabled(enable);
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
	public void addAlternateAuthWidget(Widget alternateAuthWidget) {
		table.setWidget(6, 0, alternateAuthWidget);
		table.getFlexCellFormatter().setHorizontalAlignment(5, 0,
				HasHorizontalAlignment.ALIGN_CENTER);
		table.getFlexCellFormatter().setColSpan(5, 0, 2);
	}
}

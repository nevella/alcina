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
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * 
 * @author Nick Reddel
 */
public class CancellableRemoteDialog extends GlassDialogBox implements
		ModalNotifier {
	public static final String CANCEL_ACTION = "cancel";

	private Label statusLabel;

	protected Button cancelButton;

	private Button retryButton;

	private String status = "";

	private Double progress;

	protected boolean initialAnimationEnabled() {
		return true;
	}

	public CancellableRemoteDialog(String msg, PermissibleActionListener l) {
		this(msg, l, true);
	}

	public CancellableRemoteDialog(String msg, PermissibleActionListener l,
			boolean autoShow) {
		if (l == null) {
			l = new PermissibleActionListener() {
				public void vetoableAction(PermissibleActionEvent evt) {
					CancellableRemoteDialog.this.hide();
				}
			};
		}
		final PermissibleActionListener lCopy = l;
		setText("Please wait...");
		setAnimationEnabled(initialAnimationEnabled());
		Grid grr = new Grid(2, 1);
		status=msg;
		statusLabel = new Label(msg);
		grr.getCellFormatter().setHorizontalAlignment(0, 0,
				HasHorizontalAlignment.ALIGN_CENTER);
		grr.getCellFormatter().setHorizontalAlignment(1, 0,
				HasHorizontalAlignment.ALIGN_CENTER);
		grr.setCellPadding(4);
		cancelButton = new Button("Cancel");
		setRetryButton(new Button("Retry"));
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				PermissibleAction action = new PermissibleAction();
				action.setActionName(CANCEL_ACTION);
				lCopy.vetoableAction(new PermissibleActionEvent(this, action));
			}
		});
		grr.setWidget(0, 0, statusLabel);
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(0);
		hp.add(cancelButton);
		hp.add(getRetryButton());
		getRetryButton().setVisible(false);
		grr.setWidget(1, 0, hp);
		setWidget(grr);
		if (autoShow) {
			centerAndShow();
		}
	}

	public void centerAndShow() {
		center();
		show();
	}

	public void setStatus(String status) {
		this.status = CommonUtils.nullToEmpty(status);
		updateStatusLabel();
	}

	private void updateStatusLabel() {
		statusLabel.setText(progress == null ? status : CommonUtils.formatJ(
				"%s - %s% complete", status,
				CommonUtils.padTwo((int) Math.round(progress * 100))));
		center();
	}

	public void setRetryButton(Button retryButton) {
		this.retryButton = retryButton;
	}

	public Button getRetryButton() {
		return retryButton;
	}

	@Override
	public void modalOn() {
		centerAndShow();
	}

	@Override
	public void modalOff() {
		hide();
	}

	@Override
	public void setMasking(boolean masking) {
		if (!masking) {
			getGlass().setOpacity(0);
		}
	}

	@Override
	public void setProgress(double progress) {
		this.progress = progress;
		updateStatusLabel();
	}
}

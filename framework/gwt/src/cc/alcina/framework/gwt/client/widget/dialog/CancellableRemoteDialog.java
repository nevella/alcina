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


import cc.alcina.framework.common.client.actions.VetoableAction;
import cc.alcina.framework.common.client.actions.VetoableActionEvent;
import cc.alcina.framework.common.client.actions.VetoableActionExtra.VetoableActionListener;

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

 public class CancellableRemoteDialog extends GlassDialogBox {
	public static final String CANCEL_ACTION = "cancel";

	private Label statusLabel;

	protected Button cancelButton;

	private Button retryButton;

	public CancellableRemoteDialog(String msg, VetoableActionListener l) {
		if (l == null) {
			l = new VetoableActionListener() {
				public void vetoableAction(VetoableActionEvent evt) {
					CancellableRemoteDialog.this.hide();
				}
			};
		}
		final VetoableActionListener lCopy = l;
		setText("Please wait...");
		setAnimationEnabled(true);
		Grid grr = new Grid(2, 1);
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
				VetoableAction action = new VetoableAction();
				action.setActionName(CANCEL_ACTION);
				lCopy.vetoableAction(new VetoableActionEvent(this, action));
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
		center();
		show();
	}

	public void setStatus(String s) {
		statusLabel.setText(s);
	}

	public void setRetryButton(Button retryButton) {
		this.retryButton = retryButton;
	}

	public Button getRetryButton() {
		return retryButton;
	}
}

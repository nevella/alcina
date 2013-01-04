package cc.alcina.framework.gwt.client.widget.dialog;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel.PositionCallback;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class Prompter implements PermissibleActionListener {
	private final String requiredMessage;

	private OkCancelDialogBox box;

	private final Callback<String> callback;

	private TextBox text;

	public Prompter(String title, String sub, String initialValue,
			String requiredMessage,
			Callback<OkCancelDialogBox> positioningCallback,
			Callback<String> callback) {
		this.requiredMessage = requiredMessage;
		this.callback = callback;
		FlowPanel fp = new FlowPanel();
		fp.setStyleName("alcina-prompt");
		// Label titleLabel = new Label(title);
		// titleLabel.setStyleName("title");
		// fp.add(titleLabel);
		Label subLabel = new Label(sub);
		subLabel.setStyleName("sub");
		fp.add(subLabel);
		text = new TextBox();
		text.setValue(initialValue);
		fp.add(text);
		this.box = new OkCancelDialogBox(title, fp, this) {
			@Override
			protected boolean showAnimated() {
				return false;
			}
		};
		if (positioningCallback != null) {
			positioningCallback.callback(box);
		}
		text.setFocus(true);
	}

	@Override
	public void vetoableAction(PermissibleActionEvent evt) {
		if (evt.getAction().equals(OkAction.INSTANCE)) {
			if (requiredMessage != null) {
				box.show();
				Window.alert(requiredMessage);
			} else {
				callback.callback(text.getValue());
			}
		} else {
		}
	}
}

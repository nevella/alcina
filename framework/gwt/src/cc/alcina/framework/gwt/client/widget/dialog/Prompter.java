package cc.alcina.framework.gwt.client.widget.dialog;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;

public class Prompter implements PermissibleActionListener, KeyDownHandler {
	private final String requiredMessage;

	private OkCancelDialogBox box;

	private final Callback<String> callback;

	private TextBoxBase text;

	public Prompter(String title, String sub, String initialValue,
			String requiredMessage,
			Callback<OkCancelDialogBox> positioningCallback,
			Callback<String> callback) {
		this(title, sub, initialValue, requiredMessage, positioningCallback,
				callback, false);
	}

	public Prompter(String title, String sub, String initialValue,
			String requiredMessage,
			Callback<OkCancelDialogBox> positioningCallback,
			Callback<String> callback, boolean area) {
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
		if (area) {
			TextArea ta = new TextArea();
			text = ta;
			ta.setVisibleLines(5);
			ta.setCharacterWidth(50);
		} else {
			text = new TextBox();
		}
		initialValue = CommonUtils.nullToEmpty(initialValue);
		text.setValue(initialValue);
		fp.add(text);
		this.box = new OkCancelDialogBox(title, fp, this) {
			@Override
			protected boolean showAnimated() {
				return false;
			}
		};
		if (positioningCallback != null) {
			positioningCallback.apply(box);
		}
		text.setSelectionRange(0, initialValue.length());
		text.setFocus(true);
		text.addKeyDownHandler(this);
	}

	@Override
	public void vetoableAction(PermissibleActionEvent evt) {
		if (evt.getAction().equals(OkAction.INSTANCE)) {
			if (requiredMessage != null
					&& CommonUtils.isNullOrEmpty(text.getValue())) {
				box.show();
				Window.alert(requiredMessage);
				box.okButton.setEnabled(true);
			} else {
				callback.apply(text.getValue());
			}
		} else {
		}
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		int nativeKeyCode = event.getNativeKeyCode();
		if (nativeKeyCode == KeyCodes.KEY_ESCAPE) {
			event.preventDefault();
			event.stopPropagation();
			box.hide();
		} else if (nativeKeyCode == KeyCodes.KEY_ENTER) {
			event.preventDefault();
			event.stopPropagation();
			box.hide();
			callback.apply(text.getValue());
		}
	}
}

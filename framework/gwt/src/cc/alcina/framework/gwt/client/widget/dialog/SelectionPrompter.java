package cc.alcina.framework.gwt.client.widget.dialog;

import java.util.Map;
import java.util.Map.Entry;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.widget.Link;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class SelectionPrompter<T> implements PermissibleActionListener {

	private OkCancelDialogBox box;

	private final Callback<T> callback;

	private ClickHandler clickHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {
			box.hide();
			callback.callback((T) ((Link)event.getSource()).getUserObject());
					
		}
	};

	public SelectionPrompter(String title, String sub,
			Map<String, Object> keyValues,
			Callback<OkCancelDialogBox> positioningCallback,
			Callback<T> callback) {
		this.callback = callback;
		FlowPanel fp = new FlowPanel();
		fp.setStyleName("alcina-prompt");
		// Label titleLabel = new Label(title);
		// titleLabel.setStyleName("title");
		// fp.add(titleLabel);
		Label subLabel = new Label(sub);
		subLabel.setStyleName("sub");
		fp.add(subLabel);
		FlowPanel kvHolder = new FlowPanel();
		kvHolder.setStyleName("kv-holder");
		fp.add(kvHolder);
		for (Entry<String, Object> kv : keyValues.entrySet()) {
			Link link = new Link(kv.getKey(), clickHandler);
			link.setStyleName("link-no-underline");
			link.getElement().getStyle().setDisplay(Display.BLOCK);
			link.setUserObject(kv.getValue());
			kvHolder.add(link);
		}
		this.box = new CancelDialogBox(title, fp, this) {
			@Override
			protected boolean showAnimated() {
				return false;
			}
		};
		if (positioningCallback != null) {
			positioningCallback.callback(box);
		}
	}

	@Override
	public void vetoableAction(PermissibleActionEvent evt) {
		callback.callback(null);
	}
}

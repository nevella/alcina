package cc.alcina.framework.gwt.client.data.view;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class KeyboardActionHandler implements Handler, NativePreviewHandler {
	private HandlerRegistration nativeHandlerRegistration;

	private Widget widget;

	private char key;

	private Runnable runnable;

	@Override
	public void onAttachOrDetach(AttachEvent event) {
		if (event.isAttached()) {
			this.nativeHandlerRegistration = Event
					.addNativePreviewHandler(this);
		} else {
			if (nativeHandlerRegistration != null) {
				nativeHandlerRegistration.removeHandler();
				nativeHandlerRegistration = null;
			}
		}
	}

	@Override
	public void onPreviewNativeEvent(NativePreviewEvent event) {
		if (event.getNativeEvent().getType().equals("keyup")) {
			if (WidgetUtils.isVisibleAncestorChain(widget)) {
				int keyCode = event.getNativeEvent().getKeyCode();
				if (event.getNativeEvent().getCtrlKey()
						&& keyCode == (int) key) {
					runnable.run();
				}
			}
		}
	}

	public void setup(Widget widget, char key, Runnable runnable) {
		this.widget = widget;
		this.key = key;
		this.runnable = runnable;
		widget.addAttachHandler(this);
	}
}
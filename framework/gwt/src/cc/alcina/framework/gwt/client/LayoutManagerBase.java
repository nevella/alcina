package cc.alcina.framework.gwt.client;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.widget.layout.Ui1LayoutEvents;
import cc.alcina.framework.gwt.client.widget.layout.Ui1LayoutEvents.LayoutEvent;
import cc.alcina.framework.gwt.client.widget.layout.Ui1LayoutEvents.LayoutEventListener;

// FIXME - 2023 - remove?
public abstract class LayoutManagerBase
		implements LayoutEventListener, ResizeHandler {
	private boolean displayInitialised = false;

	private int lastWidth, lastHeight;

	public LayoutManagerBase() {
		Window.addResizeHandler(this);
		Ui1LayoutEvents.get().addLayoutEventListener(this);
		Registry.register().singleton(LayoutManagerBase.class, this);
	}

	public boolean isDisplayInitialised() {
		return displayInitialised;
	}

	public boolean isLayoutInitialising() {
		return !displayInitialised;
	}

	@Override
	public void onLayoutEvent(LayoutEvent event) {
		if (isLayoutInitialising()) {
			return;
		}
		onWindowResized(Window.getClientWidth(), Window.getClientHeight(),
				false);
	}

	@Override
	public void onResize(ResizeEvent event) {
		onWindowResized(Window.getClientWidth(), Window.getClientHeight(),
				true);
	}

	// prevents old IE resize infinite loop
	protected boolean onWindowResized(int clientWidth, int clientHeight,
			boolean fromBrowser) {
		if (fromBrowser && lastWidth == clientWidth
				&& lastHeight == clientHeight) {
			return false;
		}
		lastWidth = clientWidth;
		lastHeight = clientHeight;
		return !isLayoutInitialising();
	}

	public abstract void redrawLayout();

	public void setDisplayInitialised(boolean displayInitialised) {
		this.displayInitialised = displayInitialised;
	}
}

package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEvent;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEventListener;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;

public abstract class LayoutManagerBase implements LayoutEventListener,
		ResizeHandler {
	public void setDisplayInitialised(boolean displayInitialised) {
		this.displayInitialised = displayInitialised;
	}

	public boolean isDisplayInitialised() {
		return displayInitialised;
	}

	public LayoutManagerBase() {
		Window.addResizeHandler(this);
		LayoutEvents.get().addLayoutEventListener(this);
		Registry.putSingleton(LayoutManagerBase.class, this);
	}

	private boolean displayInitialised = false;

	public void onResize(ResizeEvent event) {
		onWindowResized(Window.getClientWidth(), Window.getClientHeight(), true);
	}

	public abstract void redrawLayout();

	private int lastWidth, lastHeight;

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

	public void onLayoutEvent(LayoutEvent event) {
		if (isLayoutInitialising()) {
			return;
		}
		onWindowResized(Window.getClientWidth(), Window.getClientHeight(),
				false);
	}

	public boolean isLayoutInitialising() {
		return !displayInitialised;
	}
}

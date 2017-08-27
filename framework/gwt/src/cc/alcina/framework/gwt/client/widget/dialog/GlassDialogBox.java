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

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ScrollEvent;
import com.google.gwt.user.client.Window.ScrollHandler;
import com.google.gwt.user.client.ui.DialogBox;

import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;
import cc.alcina.framework.gwt.client.widget.GlassDisplayer;

/**
 * 
 * @author Nick Reddel
 */
public class GlassDialogBox extends DialogBox {
	@Override
	public void hide() {
		if (HIDE_INSTANTLY) {
			setAnimationEnabled(false);
		}
		super.hide();
		forgetScrollback();
		glass.show(false);
	}

	private boolean glassHidden;

	public boolean isGlassHidden() {
		return this.glassHidden;
	}

	public void setGlassHidden(boolean glassHidden) {
		this.glassHidden = glassHidden;
		if (glassHidden) {
			addStyleName("glass-hidden");
		}
	}

	public static boolean HIDE_INSTANTLY = false;

	private GlassDisplayer glass = new GlassDisplayer();

	private int scrollLeft;

	private int scrollTop;

	public GlassDialogBox() {
		setAutoHideOnHistoryEventsEnabled(true);
	}

	private AtEndOfEventSeriesTimer scrollBackTimer = new AtEndOfEventSeriesTimer(
			100, new Runnable() {
				@Override
				public void run() {
					if (getOffsetHeight() < Window.getClientHeight() - 40) {
						Window.scrollTo(scrollLeft, scrollTop);
					}
				}
			});

	private ScrollHandler scrollHandler = new ScrollHandler() {
		@Override
		public void onWindowScroll(ScrollEvent event) {
			if (scrollLeft != Window.getScrollLeft()
					|| scrollTop != Window.getScrollTop()) {
				scrollBackTimer.triggerEventOccurred();
			}
		}
	};;;

	protected void onDetach() {
		super.onDetach();
		forgetScrollback();
	};

	private void forgetScrollback() {
		if (handlerRegistration != null) {
			handlerRegistration.removeHandler();
			handlerRegistration = null;
		}
		if (scrollBackTimer != null) {
			scrollBackTimer.cancel();
		}
	}

	private HandlerRegistration handlerRegistration;

	public GlassDisplayer getGlass() {
		return this.glass;
	}

	@Override
	// glass won't be visible, but will be added to DOM before dialog
	public void center() {
		if (!isGlassHidden()) {
			glass.show(true);
		}
		super.center();
	}

	@Override
	public void show() {
		if (!isGlassHidden()) {
			glass.show(true);
		}
		scrollLeft = Window.getScrollLeft();
		scrollTop = Window.getScrollTop();
		if (this.handlerRegistration == null && !BrowserMod.isMobile()) {
			this.handlerRegistration = Window
					.addWindowScrollHandler(scrollHandler);
		}
		super.show();
	}

	@Override
	protected void onPreviewNativeEvent(NativePreviewEvent event) {
		if (event.isFirstHandler()) {
			Event as = Event.as(event.getNativeEvent());
			int typeInt = as.getTypeInt();
			if ((typeInt & Event.KEYEVENTS) > 0) {
				if (as.getCtrlKey() || as.getMetaKey() || as.getAltKey()) {
					event.consume();
				}
			}
		}
		super.onPreviewNativeEvent(event);
	}
}

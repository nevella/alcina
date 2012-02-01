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

import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;
import cc.alcina.framework.gwt.client.widget.GlassDisplayer;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ScrollEvent;
import com.google.gwt.user.client.Window.ScrollHandler;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * 
 * @author Nick Reddel
 */
public class GlassDialogBox extends DialogBox {
	@Override
	public void hide() {
		super.hide();
		forgetScrollback();
		glass.show(false);
	}

	private GlassDisplayer glass = new GlassDisplayer();

	private int scrollLeft;

	private int scrollTop;

	private AtEndOfEventSeriesTimer scrollBackTimer = new AtEndOfEventSeriesTimer(
			100, new Runnable() {
				@Override
				public void run() {
					Window.scrollTo(scrollLeft, scrollTop);
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
		glass.show(true);
		super.center();
	}

	@Override
	public void show() {
		glass.show(true);
		scrollLeft = Window.getScrollLeft();
		scrollTop = Window.getScrollTop();
		if (this.handlerRegistration == null) {
			this.handlerRegistration = Window
					.addWindowScrollHandler(scrollHandler);
		}
		super.show();
	}
}

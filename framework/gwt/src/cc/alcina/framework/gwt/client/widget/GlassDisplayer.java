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
package cc.alcina.framework.gwt.client.widget;

import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * 
 * @author Nick Reddel
 */
public class GlassDisplayer {
	public static int DEFAULT_OPACITY = 50;

	private int opacity = GlassDisplayer.DEFAULT_OPACITY;

	private PopupPanel glass;

	private FlowPanelClickable fp;
	
	

	public void show(boolean show) {
		RootPanel.get().setStyleName("glass-showing", show);
		if (!show) {
			if (glass != null) {
				glass.hide();
			}
			return;
		}
		if (glass == null) {
			glass = new PopupPanel();
			fp = new FlowPanelClickable();
			fp.setStyleName("alcina-GlassPanel");
			fp.setWidth(Window.getClientWidth() + "px");
			fp.setHeight(Math.max(Document.get().getBody().getOffsetHeight(),
					Window.getClientHeight()) + "px");
			Style style = fp.getElement().getStyle();
			style.setBackgroundColor("#000");
			updateOpacity();
			glass.setStyleName("alcina-GlassPopup");
			glass.add(fp);
			glass.setAnimationEnabled(false);
		}
		glass.show();
	}

	private void updateOpacity() {
		if (fp != null) {
			WidgetUtils.setOpacity(fp, opacity);
		}
	}

	public void setOpacity(int opacity) {
		this.opacity = opacity;
		updateOpacity();
	}

	public int getOpacity() {
		return opacity;
	}

	public void setModal(boolean modal) {
		this.glass.setModal(modal);
	}

	public FlowPanelClickable getFp() {
		return this.fp;
	}
}

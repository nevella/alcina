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

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class GlassDisplayer {
	public static int DEFAULT_OPACITY = 50;

	private int opacity = GlassDisplayer.DEFAULT_OPACITY;

	private PopupPanel glass;

	private FlowPanel fp;

	public void show(boolean show) {
		if (!show) {
			if (glass != null) {
				glass.hide();
			}
			return;
		}
		if (glass == null) {
			glass = new PopupPanel();
			fp = new FlowPanel();
			fp.setStyleName("alcina-GlassPanel");
			fp.setWidth(Window.getClientWidth() + "px");
			fp.setHeight(Math.max(Document.get().getBody().getOffsetHeight(),
					Window.getClientHeight())
					+ "px");
			DOM.setStyleAttribute(fp.getElement(), "backgroundColor", "#000");
			updateOpacity();
			glass.setStyleName("");
			glass.add(fp);
			glass.setAnimationEnabled(false);
		}
		glass.show();
	}
	private void updateOpacity(){
		WidgetUtils.setOpacity(fp, opacity);
	}
	public void setOpacity(int opacity) {
		this.opacity = opacity;
		updateOpacity();
	}

	public int getOpacity() {
		return opacity;
	}
}

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
package cc.alcina.framework.gwt.client.widget.layout;

import java.util.Iterator;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author Nick Reddel
 */
public class FlowPanel100pcHeight extends FlowPanel
		implements HasLayoutInfo, HasClickHandlers {
	@Override
	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	public LayoutInfo getLayoutInfo() {
		return new LayoutInfo() {
			@Override
			public int getClientAdjustHeight() {
				return getClientAdjustHeightFp();
			}

			@Override
			public Iterator<Widget> getLayoutWidgets() {
				return FlowPanel100pcHeight.this.iterator();
			}

			public boolean to100percentOfAvailableHeight() {
				return true;
			}
		};
	}

	protected int getClientAdjustHeightFp() {
		return 0;
	}
}
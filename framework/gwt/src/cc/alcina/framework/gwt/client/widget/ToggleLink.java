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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

/**
 *
 * @author Nick Reddel
 */
public class ToggleLink extends Composite implements
		HasSelectionHandlers<Integer>, ClickHandler {
	private FlowPanel fp;

	private Link link1;

	private Link link2;

	private int selectedIndex;
	public ToggleLink(String state1, String state2,
			SelectionHandler<Integer> handler) {
		this(state1,state2,handler,0);
	}
	public ToggleLink(String state1, String state2,
			SelectionHandler<Integer> handler,int initialIndex) {
		this.fp = new FlowPanel();
		this.link1 = new Link(state1, this);
		this.link2 = new Link(state2, this);
		fp.add(link1);
		fp.add(link2);
		setSelectedIndex(initialIndex);
		addSelectionHandler(handler);
		initWidget(fp);
	}

	public void addButtonStyleName(String style) {
		link1.addStyleName(style);
		link2.addStyleName(style);
	}

	private void updateVisibility() {
		link1.setVisible(selectedIndex == 0);
		link2.setVisible(selectedIndex == 1);
	}

	public int getSelectedIndex() {
		return this.selectedIndex;
	}

	public void setSelectedIndex(int selectedIndex) {
		this.selectedIndex = selectedIndex;
		updateVisibility();
	}

	public HandlerRegistration addSelectionHandler(
			SelectionHandler<Integer> handler) {
		return addHandler(handler, SelectionEvent.getType());
	}

	public void onClick(ClickEvent event) {
		setSelectedIndex(Math.abs((event.getSource() == link1 ? 0 : 1) - 1));
		SelectionEvent.fire(this, selectedIndex);
	}
}

package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class ToggleLink extends Composite implements
		HasSelectionHandlers<Integer>, ClickHandler {
	private FlowPanel fp;

	private Link link1;

	private Link link2;

	public ToggleLink(String state1, String state2,
			SelectionHandler<Integer> handler) {
		this.fp = new FlowPanel();
		this.link1 = new Link(state1, this);
		this.link2 = new Link(state2, this);
		fp.add(link1);
		fp.add(link2);
		updateVisibility(0);
		addSelectionHandler(handler);
		initWidget(fp);
	}

	private void updateVisibility(int i) {
		link1.setVisible(i == 0);
		link2.setVisible(i == 1);
	}

	public HandlerRegistration addSelectionHandler(
			SelectionHandler<Integer> handler) {
		return addHandler(handler, SelectionEvent.getType());
	}

	public void onClick(ClickEvent event) {
		int selectedValue = event.getSource() == link1 ? 0 : 1;
		updateVisibility(Math.abs(selectedValue - 1));
		SelectionEvent.fire(this, selectedValue);
	}
}

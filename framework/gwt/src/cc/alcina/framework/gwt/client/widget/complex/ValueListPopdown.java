package cc.alcina.framework.gwt.client.widget.complex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.widget.BlockLink;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.Renderer;

public class ValueListPopdown<T> {
	private PopupPanel popupPanel;

	private PopdownSelect select;

	public void showPopdown(Widget eventSource, Collection<T> values,
			Renderer renderer, Callback<T> callback) {
		ArrayList<T> list = new ArrayList<T>(values);
		showPopdown(eventSource, (T[]) list.toArray(), renderer, callback);
	}

	public void showPopdown(Widget eventSource, T[] values, Renderer renderer,
			Callback<T> callback) {
		if (popupPanel != null) {
			popupPanel.hide();
		}
		popupPanel = new PopupPanel(true);
		popupPanel.setStyleName("");
		select = new PopdownSelect(renderer, values, callback);
		popupPanel.add(select);
		popupPanel.setPopupPosition(-5000, -5000);
		popupPanel.show();
		popupPanel.setPopupPosition(
				eventSource.getAbsoluteLeft() - 2,
				eventSource.getAbsoluteTop() + 2
						+ eventSource.getOffsetHeight());
		select.getElement().getStyle()
				.setPropertyPx("minWidth", eventSource.getOffsetWidth() - 14);
	}

	class PopdownSelect extends Composite implements ClickHandler {
		private FlowPanel fp;

		private final Callback<T> callback;

		public PopdownSelect(Renderer r, List<T> availableValues,
				Callback<T> callback) {
			this(r, (T[]) availableValues.toArray(), callback);
		}

		public PopdownSelect(Renderer r, T[] availableValues,
				Callback<T> callback) {
			this.callback = callback;
			this.fp = new FlowPanel();
			fp.setStyleName("popdown-select");
			for (Object o : availableValues) {
				BlockLink nh = new BlockLink(r.render(o).toString());
				nh.setStyleName("block");
				nh.setUserObject(o);
				nh.addClickHandler(this);
				fp.add(nh);
			}
			initWidget(fp);
		}

		public void onClick(ClickEvent clickEvent) {
			Widget sender = (Widget) clickEvent.getSource();
			callback.apply((T) ((BlockLink) sender).getUserObject());
			popupPanel.hide();
		}
	}
}

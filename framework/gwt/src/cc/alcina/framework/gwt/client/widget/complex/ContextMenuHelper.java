package cc.alcina.framework.gwt.client.widget.complex;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class ContextMenuHelper {
	public FlexTable addRow(FlowPanel fp, Widget image, Widget contentWidget,
			final ClickHandler clickHandler, boolean separatorAbove) {
		FlexTable ft = new FlexTable();
		ft.setCellPadding(0);
		ft.setCellSpacing(0);
		ft.setStyleName("tools-table");
		if (image != null) {
			// i.e. has action
			ft.addStyleName("action");
		}
		FlexCellFormatter fcf = ft.getFlexCellFormatter();
		if (clickHandler != null) {
			ClickHandler interceptor = new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					event.preventDefault();
					clickHandler.onClick(event);
				}
			};
			ft.addClickHandler(interceptor);
		}
		if (image != null) {
			ft.setWidget(0, 0, image);
			ft.setWidget(0, 1, contentWidget);
			fcf.setStyleName(0, 0, "tools-cell-left");
		} else {
			ft.setWidget(0, 0, contentWidget);
		}
		if (separatorAbove) {
			ft.addStyleName("separator-above");
		}
		fp.add(ft);
		return ft;
	}
}

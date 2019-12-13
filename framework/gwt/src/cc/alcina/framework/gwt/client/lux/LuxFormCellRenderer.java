package cc.alcina.framework.gwt.client.lux;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.gwittir.widget.GridFormCellRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.PasswordTextBox;
import cc.alcina.framework.gwt.client.gwittir.widget.TextBox;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.widget.FormLabel;

public class LuxFormCellRenderer implements GridFormCellRenderer {
	private FlowPanel panel;

	List<Widget> boundWidgets = new ArrayList<>();

	public LuxFormCellRenderer() {
		this.panel = LuxWidgets.with(new FlowPanel())
				.withStyle(LuxFormStyle.LUX_FORM).build();
	}

	@Override
	public void addButtonWidget(Widget widget) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Widget> T getBoundWidget(int row) {
		return (T) boundWidgets.get(row);
	}

	@Override
	public Widget getWidget() {
		return panel;
	}

	@Override
	public void renderCell(Field field, int row, int col, Widget widget) {
		boundWidgets.add(widget);
		FlowPanel container = LuxWidgets.with(new FlowPanel())
				.withStyle(LuxFormStyle.LUX_FORM_ELEMENT).build();
		FormLabel formLabel = LuxWidgets.with(new FormLabel(field.getLabel()))
				.withStyle(LuxFormStyle.LUX_FORM_LABEL).build();
		widget.getElement().ensureId();
		String widgetId = widget.getElement().getId();
		formLabel.setFor(widgetId);
		container.add(widget);
		// add label after widget!
		container.add(formLabel);
		if (Ax.notBlank(field.getStyleName())) {
			container.addStyleName(field.getStyleName());
		}
		if (Ax.notBlank(field.getWidgetStyleName())) {
			widget.addStyleName(field.getWidgetStyleName());
			container.addStyleName(field.getWidgetStyleName());
		}
		if (field.getHelpText() != null) {
			formLabel.addStyleName("has-help-text");
			formLabel.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Widget sender = (Widget) event.getSource();
					final PopupPanel p = new PopupPanel(true);
					p.setStyleName("gwittir-GridForm-Help");
					p.setWidget(new HTML(field.getHelpText()));
					p.setPopupPosition(sender.getAbsoluteLeft(),
							sender.getAbsoluteTop() + sender.getOffsetHeight());
					p.show();
				}
			});
		}
		if (widget instanceof TextBox || widget instanceof PasswordTextBox
				|| widget instanceof com.totsp.gwittir.client.ui.TextBox) {
			LuxFormStyle.TEXT.add(container);
			widget.getElement().setPropertyString("placeholder", " ");
			if (Ax.notBlank(field.getAutocompleteName())) {
				widget.getElement().setPropertyString("autocomplete",
						field.getAutocompleteName());
			}
		}
		if (widget instanceof FlatSearchSelector) {
			LuxFormStyle.SELECTOR.add(container);
		}
		container.getElement().setPropertyString("container-name",
				field.getPropertyName());
		panel.add(container);
	}

	@Override
	public void setRowVisibility(int row, boolean visible) {
		throw new UnsupportedOperationException();
	}
}

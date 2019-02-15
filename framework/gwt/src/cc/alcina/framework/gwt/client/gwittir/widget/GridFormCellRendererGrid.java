package cc.alcina.framework.gwt.client.gwittir.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;
import cc.alcina.framework.gwt.client.widget.FlowPanelClickable;

public class GridFormCellRendererGrid implements GridFormCellRenderer {
    private FlexTable base = new FlexTable();

    private boolean horizontalGrid;

    public GridFormCellRendererGrid(boolean horizontalGrid) {
        this.horizontalGrid = horizontalGrid;
        if (horizontalGrid) {
            base.addStyleName("horizontal-grid");
        }
        base.setStyleName(GridForm.STYLE_NAME);
    }

    @Override
    public void addButtonWidget(Widget widget) {
        base.setWidget(row(base.getRowCount(), 1), col(base.getRowCount(), 1),
                widget);
    }

    @Override
    public <T extends Widget> T getBoundWidget(int row) {
        return (T) base.getWidget(row(row, 1), col(row, 1));
    }

    @Override
    public Widget getWidget() {
        return base;
    }

    @Override
    public void renderCell(Field field, int row, int col, Widget widget) {
        FlowPanelClickable label = new FlowPanelClickable();
        label.add(new InlineHTML(field.getLabel()));
        int vRow = row(row, col * 2);
        int vCol = col(row, col * 2);
        int vRowPlus1 = row(row, col * 2 + 1);
        int vColPlus1 = col(row, col * 2 + 1);
        this.base.setWidget(vRow, vCol, label);
        this.base.getCellFormatter().setStyleName(vRow, vCol, "label");
        boolean multiline = ((widget instanceof MultilineWidget)
                && ((MultilineWidget) widget).isMultiline());
        if (multiline) {
            this.base.getCellFormatter().addStyleName(vRow, vCol,
                    "multiline-field");
        }
        if (Ax.notBlank(field.getWidgetStyleName())) {
            widget.addStyleName(field.getWidgetStyleName());
        }
        this.base.setWidget(vRowPlus1, vColPlus1, widget);
        this.base.getCellFormatter().setStyleName(vRowPlus1, vColPlus1,
                "field");
        if (field.getHelpText() != null) {
            label.addStyleName("has-help-text");
            label.addClickHandler(new ClickHandler() {
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
        if (field.getStyleName() != null) {
            this.base.getCellFormatter().addStyleName(vRowPlus1, vColPlus1,
                    field.getStyleName());
        }
    }

    @Override
    public void setRowVisibility(int row, boolean visible) {
        if (horizontalGrid) {
            base.getColumnFormatter().setVisible(row, visible);
        } else {
            base.getRowFormatter().setVisible(row, visible);
        }
    }

    private int col(int vRow, int vCol) {
        return horizontalGrid ? vRow : vCol;
    }

    private int row(int vRow, int vCol) {
        return horizontalGrid ? vCol : vRow;
    }
}
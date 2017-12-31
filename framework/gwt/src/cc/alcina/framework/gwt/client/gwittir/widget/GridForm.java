/*
 * GridForm.java
 *
 * Created on August 7, 2007, 7:15 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package cc.alcina.framework.gwt.client.gwittir.widget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.action.BindingAction;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.HasDefaultBinding;
import com.totsp.gwittir.client.ui.table.AbstractTableWidget;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.gwittir.HasBinding;
import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.logic.RenderContext;
import cc.alcina.framework.gwt.client.widget.FlowPanelClickable;

/**
 * 
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 * @author Nick Reddel
 *         <p>
 *         Changes to gwittir.GridForm:
 *         </p>
 *         <ul>
 *         <li>Render labels as HTML, not Label (basically to support in-app
 *         localisation buttons)</li>
 *         <li>Add the "directSetModelDisabled" code, which is necessary for the
 *         ChildBean customiser (essentially nested grid forms)</li>
 *         <li>Implement HasBinding (necessary for PaneWrapperWithObjects
 *         factory binding)</li>
 *         <li>Add debugIds and multline rendering prettiness to the render
 *         method</li>
 *         </ul>
 * 
 */
@SuppressWarnings("deprecation")
public class GridForm extends AbstractTableWidget
		implements HasDefaultBinding, HasBinding {
	private static final String STYLE_NAME = "gwittir-GridForm";

	private static final BindingAction DEFAULT_ACTION = new BindingAction() {
		public void bind(BoundWidget widget) {
			try {
				((HasDefaultBinding) widget).bind();
			} catch (ClassCastException cce) {
				throw new RuntimeException(cce);
			}
		}

		public void execute(BoundWidget model) {
		}

		public void set(BoundWidget widget) {
			try {
				((GridForm) widget).set();
			} catch (ClassCastException cce) {
				throw new RuntimeException(cce);
			}
		}

		public void unbind(BoundWidget widget) {
			try {
				((GridForm) widget).unbind();
			} catch (ClassCastException cce) {
				throw new RuntimeException(cce);
			}
		}
	};

	private Binding binding = new Binding();

	private FlexTable base = new FlexTable();

	private Field[] fields;

	private int columns = 1;

	private boolean directSetModelDisabled = false;

	private Focusable focusOnDetachIfEditorFocussed;

	private Object focussedWidget;

	private boolean horizontalGrid;

	private FocusHandler focusHandler = new FocusHandler() {
		public void onFocus(FocusEvent event) {
			Object source = event.getSource();
			focussedWidget = source;
		}
	};

	private BlurHandler blurHandler = new BlurHandler() {
		public void onBlur(BlurEvent event) {
			if (focussedWidget == event.getSource()) {
				focussedWidget = null;
			}
		}
	};

	private FocusListener focusListener = new FocusListener() {
		public void onFocus(Widget sender) {
			focussedWidget = sender;
		}

		public void onLostFocus(Widget sender) {
			if (focussedWidget == sender) {
				focussedWidget = null;
			}
		}
	};

	private Field autofocusField;

	private Widget autofocusWidget;

	public GridForm(Field[] fields, int columns, BoundWidgetTypeFactory factory,
			boolean horizontalGrid) {
		this.fields = fields;
		this.columns = columns;
		this.factory = factory;
		this.horizontalGrid = horizontalGrid;
		super.initWidget(this.base);
		this.setStyleName(GridForm.STYLE_NAME);
		if (horizontalGrid) {
			this.addStyleName("horizontal-grid");
		}
		this.setAction(GridForm.DEFAULT_ACTION);
	}

	public void addButtonWidget(Widget widget) {
		base.setWidget(row(base.getRowCount(), 1), col(base.getRowCount(), 1),
				widget);
	}

	public void bind() {
		this.binding.bind();
	}

	public Field getAutofocusField() {
		return this.autofocusField;
	}

	public Binding getBinding() {
		return this.binding;
	}

	public <T extends Widget> T getBoundWidget(int row) {
		return (T) base.getWidget(row(row, 1), col(row, 1));
	}

	public int getCaptionColumnWidth() {
		if (this.fields.length == 0) {
			return 0;
		}
		return base.getFlexCellFormatter().getElement(0, 0).getOffsetWidth();
	}

	public Field[] getFields() {
		return this.fields;
	}

	public Focusable getFocusOnDetachIfEditorFocussed() {
		return this.focusOnDetachIfEditorFocussed;
	}

	public Object getValue() {
		return this.getModel();
	}

	public boolean isDirectSetModelDisabled() {
		return directSetModelDisabled;
	}

	public void set() {
		this.binding.setLeft();
	}

	public void setAutofocusField(Field autofocusField) {
		this.autofocusField = autofocusField;
	}

	public void setCaptionColumnWidth(int pixelWidth) {
		base.getFlexCellFormatter().setWidth(0, 0, pixelWidth + "px");
	}

	public void setDirectSetModelDisabled(boolean directSetModelDisabled) {
		this.directSetModelDisabled = directSetModelDisabled;
	}

	public void setFocusOnDetachIfEditorFocussed(
			Focusable focusOnDetachIfEditorFocussed) {
		this.focusOnDetachIfEditorFocussed = focusOnDetachIfEditorFocussed;
	}

	public void setModel(Object model) {
		setModel(model, true);
	}

	public void setRowVisibility(int row, boolean visible) {
		if (horizontalGrid) {
			base.getColumnFormatter().setVisible(row, visible);
		} else {
			base.getRowFormatter().setVisible(row, visible);
		}
	}

	public void setValue(Object value) {
		Object old = this.getModel();
		this.setModel(value, false);
		this.changes.firePropertyChange("value", old, value);
	}

	public void unbind() {
		this.binding.unbind();
	}

	private int col(int vRow, int vCol) {
		return horizontalGrid ? vRow : vCol;
	}

	private void render() {
		if (this.binding.getChildren().size() > 0) {
			this.binding.unbind();
			this.binding.getChildren().clear();
		}
		int row = 0;
		for (int i = 0; i < this.fields.length;) {
			for (int col = 0; (col < this.columns)
					&& (i < fields.length); col++) {
				final Field field = this.fields[i];
				if (field == null) {
					i++;
					continue;
				}
				Widget widget = (Widget) this.createWidget(this.binding, field,
						(SourcesPropertyChangeEvents) this.getValue());
				if (field == autofocusField) {
					autofocusWidget = widget;
				}
				if (widget instanceof HasAllFocusHandlers) {
					HasAllFocusHandlers haff = (HasAllFocusHandlers) widget;
					haff.addBlurHandler(blurHandler);
					haff.addFocusHandler(focusHandler);
				} else if (widget instanceof HasFocus) {
					HasFocus hf = (HasFocus) widget;
					hf.addFocusListener(focusListener);
				}
				widget.ensureDebugId(AlcinaDebugIds.GRID_FORM_FIELD_DEBUG_PREFIX
						+ field.getPropertyName());
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
						public void onClick(ClickEvent event) {
							Widget sender = (Widget) event.getSource();
							final PopupPanel p = new PopupPanel(true);
							p.setStyleName("gwittir-GridForm-Help");
							p.setWidget(new HTML(field.getHelpText()));
							p.setPopupPosition(sender.getAbsoluteLeft(),
									sender.getAbsoluteTop()
											+ sender.getOffsetHeight());
							p.show();
						}
					});
				}
				if (field.getStyleName() != null) {
					this.base.getCellFormatter().addStyleName(vRowPlus1,
							vColPlus1, field.getStyleName());
				}
				i++;
			}
			row++;
		}
	}

	private int row(int vRow, int vCol) {
		return horizontalGrid ? vCol : vRow;
	}

	private void setModel(Object model, boolean direct) {
		if (direct && directSetModelDisabled) {
			return;
		}
		super.setModel(model);
		this.render();
	}

	@Override
	protected void onLoad() {
		super.onLoad();
		if (autofocusWidget instanceof Focusable && !RenderContext.get()
				.getBoolean(RenderContext.CONTEXT_IGNORE_AUTOFOCUS)) {
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					((Focusable) autofocusWidget).setFocus(true);
				}
			});
		}
	}

	protected void onUnload() {
		if (focussedWidget != null && focusOnDetachIfEditorFocussed != null) {
			focusOnDetachIfEditorFocussed.setFocus(true);
		}
	}
}

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
package cc.alcina.framework.gwt.client.widget.complex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.HasMouseOutHandlers;
import com.google.gwt.event.dom.client.HasMouseOverHandlers;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.table.DataProvider;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.BasicBindingAction;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.gwittir.RequiresContextBindable;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.gwittir.widget.EndRowButtonClickedEvent;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupAxis;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;

/**
 *
 * @author Nick Reddel
 */
public class FastROBoundTable extends BoundTableExt {
	Map<String, BoundWidgetProvider> wpMap = new HashMap<String, BoundWidgetProvider>();

	Map<String, Property> pMap = new HashMap<String, Property>();

	private List selectedObjects = new ArrayList();

	public boolean reallyClear;

	public Predicate<CheckEditableTuple> checkEditableFilter;

	private EditOverlayHandler editOverlayHandler;

	private int lastSelectedIndex = -1;

	private Object selectedObject = null;

	public FastROBoundTable(int mask, Field[] fields, DataProvider provider) {
		super(mask, fields, provider);
	}

	@Override
	protected void addRow(final SourcesPropertyChangeEvents o) {
		int row = table.getRowCount();
		final CheckBox handle;
		int startColumn = 0;
		final List<CheckBox> handles = this.rowHandles;
		if ((this.masks & BoundTableExt.ROW_HANDLE_MASK) > 0) {
			handle = new CheckBox();
			handle.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					setActive(true);
					List newSelected = null;
					if ((masks & BoundTableExt.MULTIROWSELECT_MASK) > 0) {
						newSelected = new ArrayList(getSelectedObjects());
					} else {
						for (CheckBox cb : handles) {
							if (cb != handle) {
								cb.setValue(false);
							}
						}
						newSelected = new ArrayList();
					}
					if (!handle.getValue()) {
						newSelected.remove(o);
					} else {
						newSelected.add(o);
					}
					setSelected(newSelected);
					setSelectedObjects(newSelected);
				}
			});
			startColumn++;
			this.rowHandles.add(handle);
			this.table.setWidget(row, 0, handle);
		} else {
			handle = null;
		}
		for (int col = 0; col < this.columns.length; col++) {
			Widget widget = (Widget) createCellWidget(col, o);
			if (this.columns[col].getWidgetStyleName() != null) {
				widget.addStyleName(this.columns[col].getWidgetStyleName());
			}
			table.setWidget(row, col + startColumn, widget);
			if (this.columns[col].getStyleName() != null) {
				table.getCellFormatter().setStyleName(row, col + startColumn,
						this.columns[col].getStyleName());
			}
		}
		if ((this.masks & BoundTableExt.END_ROW_BUTTON) > 0) {
			EndRowButton endRowButton = new EndRowButton();
			table.setWidget(row, this.columns.length + startColumn,
					endRowButton);
			int f_row = row;
			endRowButton.addClickHandler(e -> {
				EndRowButtonClickedEvent.fire(FastROBoundTable.this, f_row, o);
			});
		}
		boolean odd = (this.calculateRowToObjectOffset(Integer.valueOf(row))
				.intValue() % 2) != 0;
		this.table.getRowFormatter().setStyleName(row, odd ? "odd" : "even");
	}

	protected void beautify() {
		if (allRowsHandle != null) {
			allRowsHandle.setVisible(false);
		}
	}

	public boolean checkEditable(SourcesPropertyChangeEvents target,
			Field editableField) {
		return checkEditableFilter == null || checkEditableFilter
				.test(new CheckEditableTuple(target, editableField));
	}

	protected BoundWidget createCellWidget(int colIndex,
			SourcesPropertyChangeEvents target) {
		final BoundWidget widget;
		Field col = this.columns[colIndex];
		if (!wpMap.containsKey(col.getPropertyName())) {
			Property p = Reflections.at(target).property(col.getPropertyName());
			BoundWidgetProvider wp = this.factory
					.getWidgetProvider(p.getType());
			pMap.put(col.getPropertyName(), p);
			wpMap.put(col.getPropertyName(), wp);
		}
		Property p = pMap.get(col.getPropertyName());
		BoundWidgetProvider wp = col.getCellProvider() != null
				? col.getCellProvider()
				: wpMap.get(col.getPropertyName());
		if (wp instanceof RequiresContextBindable) {
			((RequiresContextBindable) wp).setBindable(target);
		}
		widget = wp.get();
		try {
			widget.setModel(target);
			widget.setValue(p.get(target));
		} catch (Exception e) {
			GWT.log("Exception creating cell widget", e);
		}
		return widget;
	}

	@Override
	protected FlexTable createTableImpl() {
		ROFlexTable table = new ROFlexTable();
		return table;
	}

	public void edit(Object target, final String fieldName) {
		int row = CommonUtils.indexOf(((Collection) getValue()).iterator(),
				target);
		if (row == -1) {
			return;
		}
		row += ((this.masks & BoundTableExt.HEADER_MASK) > 0) ? 1 : 0;
		List<Field> list = Arrays.asList(columns);
		Field match = list.stream()
				.filter(field -> field.getPropertyName().equals(fieldName))
				.findFirst().orElse(null);
		int col = list.indexOf(match);
		col += ((this.masks & BoundTableExt.ROW_HANDLE_MASK) > 0) ? 1 : 0;
		editOverlayHandler.edit(new RowCol(row, col));
	}

	/**
	 * Note - you may well want to make the parent of this table relative
	 */
	public void editOverlay(Class tableObjectClass) {
		ROFlexTable t = (ROFlexTable) table;
		editOverlayHandler = new EditOverlayHandler(tableObjectClass);
		t.addClickHandler(editOverlayHandler);
		t.addMouseOutHandler(editOverlayHandler);
		t.addMouseMoveHandler(editOverlayHandler);
		t.addMouseOverHandler(editOverlayHandler);
		table.addStyleName("editable");
	}

	public Predicate<CheckEditableTuple> getCheckEditableFilter() {
		return this.checkEditableFilter;
	}

	public List getSelectedObjects() {
		return this.selectedObjects;
	}

	public void redrawRowForObject(Object o) {
		List list = (List) getValue();
		Iterator itr = list.iterator();
		int i = 0;
		int startColumn = (this.masks & BoundTableExt.ROW_HANDLE_MASK) > 0 ? 1
				: 0;
		for (; itr.hasNext(); i++) {
			if (itr.next() == o) {
				break;
			}
		}
		if (i == list.size()) {
			return;
		}
		int row = calculateObjectToRowOffset(i);
		reallyClear = true;
		for (int col = 0; col < this.columns.length; col++) {
			Widget widget = (Widget) createCellWidget(col,
					(SourcesPropertyChangeEvents) o);
			table.setWidget(row, col + startColumn, widget);
		}
		reallyClear = false;
	}

	public void removeRow(Object o) {
		List list = (List) getValue();
		Iterator itr = list.iterator();
		int i = 0;
		int startColumn = (this.masks & BoundTableExt.ROW_HANDLE_MASK) > 0 ? 1
				: 0;
		for (; itr.hasNext(); i++) {
			if (itr.next() == o) {
				break;
			}
		}
		if (i == list.size()) {
			return;
		}
		int row = calculateObjectToRowOffset(i);
		table.removeRow(
				i + ((this.masks & BoundTableExt.HEADER_MASK) > 0 ? 1 : 0));
		((Collection) getValue()).remove(o);
	}

	@Override
	protected void renderAll() {
		if (wpMap == null) {
			return;
		}
		super.renderAll();
		setSelectedObject(selectedObject);
	}

	public void setCheckEditableFilter(
			Predicate<CheckEditableTuple> checkEditableFilter) {
		this.checkEditableFilter = checkEditableFilter;
	}

	public void setSelectedObject(Object object) {
		selectedObject = object;
		Iterator itr = ((Collection) getValue()).iterator();
		int rowIndex = 0;
		while (itr.hasNext()) {
			Object value = itr.next();
			if (value == object) {
				break;
			}
			rowIndex++;
		}
		int headerOffset = ((this.masks & BoundTableExt.HEADER_MASK) > 0) ? 1
				: 0;
		rowIndex += headerOffset;
		if (lastSelectedIndex != -1) {
			table.getRowFormatter().removeStyleName(lastSelectedIndex,
					"selected");
		}
		if (table.getRowCount() > rowIndex) {
			table.getRowFormatter().addStyleName(rowIndex, "selected");
			lastSelectedIndex = rowIndex;
		}
	}

	public void setSelectedObjects(List selectedObjects) {
		this.selectedObjects = selectedObjects;
	}

	public static class CheckEditableTuple {
		public SourcesPropertyChangeEvents target;

		public Field field;

		public CheckEditableTuple(SourcesPropertyChangeEvents target,
				Field editableField) {
			this.target = target;
			this.field = editableField;
		}
	}

	// this actually duplicates a lot of stuff in HTMLTable -
	private class EditOverlayHandler implements ClickHandler, MouseOutHandler,
			MouseOverHandler, MouseMoveHandler {
		Map<Integer, Boolean> editableColumns = new HashMap<Integer, Boolean>();

		BasicBindingAction action;

		RowCol lastRowCol;

		public EditOverlayHandler(Class tableObjectClass) {
			editableColumns.put(-1, false);
			int i = 0;
			if ((masks & BoundTableExt.ROW_HANDLE_MASK) > 0) {
				editableColumns.put(i++, false);
			}
			for (Field f : columns) {
				editableColumns.put(i++, Display.Support
						.isEditable(tableObjectClass, f.getPropertyName()));
			}
		}

		protected void edit(RowCol rowCol) {
			if (rowCol.row == 0 || !editableColumns.get(rowCol.col)) {
				return;
			}
			showEditable(rowCol);
			Iterator itr = ((Collection) getValue()).iterator();
			final SourcesPropertyChangeEvents target = (SourcesPropertyChangeEvents) CommonUtils
					.get(itr, rowCol.row - 1);
			int startColumn = 0;
			if ((masks & BoundTableExt.ROW_HANDLE_MASK) > 0) {
				startColumn++;
			}
			Field col = columns[rowCol.col - startColumn];
			final Field field = BeanFields.query().forClass(target.getClass())
					.forPropertyName(col.getPropertyName()).withEditable(true)
					.withAdjunctEditor(false).getField();
			if (!checkEditable(target, field)) {
				return;
			}
			BoundWidgetProvider wp = field.getCellProvider();
			if (wp instanceof RequiresContextBindable) {
				((RequiresContextBindable) wp).setBindable(target);
			}
			final BoundWidget editableWidget = wp.get();
			HasAnnotations p = Reflections.at(target)
					.property(field.getPropertyName());
			try {
				editableWidget.setModel(target);
				action = new BasicBindingAction() {
					@Override
					protected void set0(BoundWidget widget) {
						binding.getChildren().add(new Binding(widget, "value",
								field.getValidator(), field.getFeedback(),
								target, field.getPropertyName(), null, null));
						binding.setLeft();
					}
				};
				editableWidget.setAction(action);
				final BoundWidget tableWidget = (BoundWidget) table
						.getWidget(rowCol.row, rowCol.col);
				RelativePopupPanel rpp = new RelativePopupPanel(true);
				rpp.setAnimationEnabled(true);
				rpp.addStyleName("edit-overlay");
				Widget relativeToWidget = (Widget) tableWidget;
				int tdh = table.getCellFormatter()
						.getElement(rowCol.row, rowCol.col).getOffsetHeight();
				RelativePopupPanel relativePopupPanel = RelativePopupPositioning
						.showPopup(relativeToWidget, (Widget) editableWidget,
								table,
								new RelativePopupAxis[] {
										RelativePopupPositioning.BOTTOM_LTR },
								null, rpp, -4, -tdh + 4);
				relativePopupPanel.addCloseHandler(
						new CloseHandler<RelativePopupPanel>() {
							@Override
							public void onClose(
									CloseEvent<RelativePopupPanel> event) {
								tableWidget.setValue(editableWidget.getValue());
							}
						});
			} catch (Exception e) {
				GWT.log("Exception creating cell widget", e);
			}
		}

		private RowCol getRowCol(DomEvent event) {
			try {
				com.google.gwt.dom.client.Element elt = Element
						.as(event.getNativeEvent().getEventTarget());
				Element tableElt = table.getElement();
				com.google.gwt.dom.client.Element tr = null;
				com.google.gwt.dom.client.Element td = null;
				while (elt != null && elt != tableElt) {
					if (elt.getTagName().equalsIgnoreCase("td")) {
						td = elt;
					}
					if (elt.getTagName().equalsIgnoreCase("tr")) {
						tr = elt;
					}
					elt = elt.getParentElement();
				}
				return new RowCol(indexInParent(tr), indexInParent(td));
			} catch (Exception e) {
				// messy but effective
				return new RowCol(-1, -1);
			}
		}

		private int indexInParent(com.google.gwt.dom.client.Element elt) {
			com.google.gwt.dom.client.Element parent = elt.getParentElement();
			NodeList<Node> childNodes = parent.getChildNodes();
			String eltName = elt.getTagName();
			int index = -1;
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node item = childNodes.getItem(i);
				if (item.getNodeType() == Node.ELEMENT_NODE
						&& item.getNodeName().equalsIgnoreCase(eltName)) {
					index++;
				}
				if (item == elt) {
					break;
				}
			}
			return index;
		}

		@Override
		public void onClick(ClickEvent event) {
			RowCol rowCol = getRowCol(event);
			edit(rowCol);
		}

		@Override
		public void onMouseMove(MouseMoveEvent event) {
			RowCol rowCol = getRowCol(event);
			if (lastRowCol != null && !lastRowCol.equals(rowCol)) {
				showEditable(null);
			}
			if (editableColumns.get(rowCol.col)) {
				showEditable(rowCol);
			}
		}

		@Override
		public void onMouseOut(MouseOutEvent event) {
			showEditable(null);
		}

		@Override
		public void onMouseOver(MouseOverEvent event) {
		}

		protected void showEditable(RowCol rowCol) {
			styleDelta(lastRowCol, null, "editableOver");
			lastRowCol = null;
			if (rowCol != null) {
				lastRowCol = rowCol;
				styleDelta(lastRowCol, "editableOver", null);
			}
		}

		private void styleDelta(RowCol rowCol, String styleToAdd,
				String styleToRemove) {
			if (rowCol == null || rowCol.col == -1 || rowCol.row == -1) {
				return;
			}
			if (styleToAdd != null) {
				table.getCellFormatter().addStyleName(rowCol.row, rowCol.col,
						styleToAdd);
			}
			if (styleToRemove != null) {
				table.getCellFormatter().removeStyleName(rowCol.row, rowCol.col,
						styleToRemove);
			}
		}
	}

	private class ROFlexTable extends FlexTable implements HasMouseOverHandlers,
			HasMouseOutHandlers, HasMouseMoveHandlers {
		public ROFlexTable() {
			int debug = 3;
		}

		@Override
		public HandlerRegistration
				addMouseMoveHandler(MouseMoveHandler handler) {
			return addDomHandler(handler, MouseMoveEvent.getType());
		}

		@Override
		public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
			return addDomHandler(handler, MouseOutEvent.getType());
		}

		@Override
		public HandlerRegistration
				addMouseOverHandler(MouseOverHandler handler) {
			return addDomHandler(handler, MouseOverEvent.getType());
		}

		@Override
		public Element getEventTargetCell(Event event) {
			return super.getEventTargetCell(event);
		}

		@Override
		protected boolean internalClearCell(Element td,
				boolean clearInnerHTML) {
			if (!reallyClear) {
				return false;
			} else {
				return super.internalClearCell(td, clearInnerHTML);
			}
		}

		@Override
		protected void onAttach() {
			// TODO Auto-generated method stub
			super.onAttach();
		}
	}

	private class RowCol {
		int row;

		int col;

		public RowCol(int row, int col) {
			super();
			this.row = row;
			this.col = col;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof RowCol) {
				RowCol rc = (RowCol) obj;
				return row == rc.row && col == rc.col;
			}
			return false;
		}
	}
}

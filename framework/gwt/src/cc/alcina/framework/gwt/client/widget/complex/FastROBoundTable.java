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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.BasicBindingAction;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.RequiresContextBindable;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundTableExt;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.widget.dialog.RelativePopupPanel;

import com.google.gwt.core.client.GWT;
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
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.table.DataProvider;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class FastROBoundTable extends BoundTableExt {
	Map<String, BoundWidgetProvider> wpMap = new HashMap<String, BoundWidgetProvider>();

	Map<String, Property> pMap = new HashMap<String, Property>();

	private List selectedObjects = new ArrayList();

	public FastROBoundTable(int mask, BoundWidgetTypeFactory factory,
			Field[] fields, DataProvider provider) {
		super(mask, factory, fields, provider);
	}

	/**
	 * Note - you may well want to make the parent of this table relative
	 */
	public void editOverlay(Class tableObjectClass) {
		ROFlexTable t = (ROFlexTable) table;
		EditOverlayHandler handler = new EditOverlayHandler(tableObjectClass);
		t.addClickHandler(handler);
		t.addMouseOutHandler(handler);
		t.addMouseMoveHandler(handler);
		t.addMouseOverHandler(handler);
	}

	public List getSelectedObjects() {
		return this.selectedObjects;
	}

	public void setSelectedObjects(List selectedObjects) {
		this.selectedObjects = selectedObjects;
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
			table.setWidget(row, col + startColumn, widget);
		}
		boolean odd = (this.calculateRowToObjectOffset(new Integer(row))
				.intValue() % 2) != 0;
		this.table.getRowFormatter().setStyleName(row, odd ? "odd" : "even");
	}

	protected void beautify() {
		if (allRowsHandle != null) {
			allRowsHandle.setVisible(false);
		}
	}

	protected BoundWidget createCellWidget(int colIndex,
			SourcesPropertyChangeEvents target) {
		final BoundWidget widget;
		Field col = this.columns[colIndex];
		if (!wpMap.containsKey(col.getPropertyName())) {
			Property p = GwittirBridge.get().getProperty(target,
					col.getPropertyName());
			BoundWidgetProvider wp = this.factory.getWidgetProvider(
					col.getPropertyName(), p.getType());
			pMap.put(col.getPropertyName(), p);
			wpMap.put(col.getPropertyName(), wp);
		}
		Property p = pMap.get(col.getPropertyName());
		BoundWidgetProvider wp = col.getCellProvider() != null ? col
				.getCellProvider() : wpMap.get(col.getPropertyName());
		if (wp instanceof RequiresContextBindable) {
			((RequiresContextBindable) wp).setBindable(target);
		}
		widget = wp.get();
		try {
			widget.setModel(target);
			widget.setValue(p.getAccessorMethod().invoke(target,
					CommonUtils.EMPTY_OBJECT_ARRAY));
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
				editableColumns.put(
						i++,
						GwittirBridge.get().isFieldEditable(tableObjectClass,
								f.getPropertyName()));
			}
		}

		public void onClick(ClickEvent event) {
			RowCol rowCol = getRowCol(event);
			if (rowCol.row == 0 || !editableColumns.get(rowCol.col)) {
				return;
			}
			Iterator itr = ((Collection) getValue()).iterator();
			final SourcesPropertyChangeEvents target = (SourcesPropertyChangeEvents) CommonUtils
					.get(itr, rowCol.row - 1);
			int startColumn = 0;
			if ((masks & BoundTableExt.ROW_HANDLE_MASK) > 0) {
				startColumn++;
			}
			Field col = columns[rowCol.col - startColumn];
			final Field editableField = GwittirBridge.get().getField(
					target.getClass(), col.getPropertyName(), true, false);
			BoundWidgetProvider wp = editableField.getCellProvider();
			if (wp instanceof RequiresContextBindable) {
				((RequiresContextBindable) wp).setBindable(target);
			}
			final BoundWidget editableWidget = wp.get();
			Property p = GwittirBridge.get().getProperty(target,
					col.getPropertyName());
			try {
				editableWidget.setModel(target);
				action = new BasicBindingAction() {
					@Override
					protected void set0(BoundWidget widget) {
						binding.getChildren().add(
								new Binding(widget, "value", editableField
										.getValidator(), editableField
										.getFeedback(), target, editableField
										.getPropertyName(), null, null));
						binding.setLeft();
					}
				};
				editableWidget.setAction(action);
				final BoundWidget tableWidget = (BoundWidget) table.getWidget(
						rowCol.row, rowCol.col);
				RelativePopupPanel relativePopupPanel = RelativePopupPositioning
						.showPopup((Widget) tableWidget,
								(Widget) editableWidget, table,
								RelativePopupPositioning.BOTTOM_LTR);
				relativePopupPanel
						.addCloseHandler(new CloseHandler<RelativePopupPanel>() {
							public void onClose(
									CloseEvent<RelativePopupPanel> event) {
								tableWidget.setValue(editableWidget.getValue());
							}
						});
			} catch (Exception e) {
				GWT.log("Exception creating cell widget", e);
			}
		}

		public void onMouseMove(MouseMoveEvent event) {
			RowCol rowCol = getRowCol(event);
			if (lastRowCol != null && !lastRowCol.equals(rowCol)) {
				styleDelta(lastRowCol, null, "editableOver");
				lastRowCol = null;
			}
			if (editableColumns.get(rowCol.col)) {
				lastRowCol = rowCol;
				styleDelta(lastRowCol, "editableOver", null);
			}
		}

		public void onMouseOut(MouseOutEvent event) {
			styleDelta(lastRowCol, null, "editableOver");
			lastRowCol = null;
		}

		public void onMouseOver(MouseOverEvent event) {
		}

		private RowCol getRowCol(DomEvent event) {
			try {
				com.google.gwt.dom.client.Element elt = Element.as(event
						.getNativeEvent().getEventTarget());
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
				table.getCellFormatter().removeStyleName(rowCol.row,
						rowCol.col, styleToRemove);
			}
		}
	}

	private static class ROFlexTable extends FlexTable implements
			HasMouseOverHandlers, HasMouseOutHandlers, HasMouseMoveHandlers {
		public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
			return addDomHandler(handler, MouseMoveEvent.getType());
		}

		public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
			return addDomHandler(handler, MouseOutEvent.getType());
		}

		public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
			return addDomHandler(handler, MouseOverEvent.getType());
		}

		protected boolean internalClearCell(Element td, boolean clearInnerHTML) {
			return false;
		}

		@Override
		public Element getEventTargetCell(Event event) {
			return super.getEventTargetCell(event);
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

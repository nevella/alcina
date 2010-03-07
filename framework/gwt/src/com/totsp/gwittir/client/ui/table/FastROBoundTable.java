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

package com.totsp.gwittir.client.ui.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.RequiresContextBindable;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Bindable;
import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;
@SuppressWarnings("unchecked")
public class FastROBoundTable extends BoundTable {
	Map<String, BoundWidgetProvider> wpMap = new HashMap<String, BoundWidgetProvider>();

	Map<String, Property> pMap = new HashMap<String, Property>();

	public FastROBoundTable(int mask, BoundWidgetTypeFactory factory,
			Field[] fields, DataProvider provider) {
		super(mask, factory, fields, provider);
	}

	protected BoundWidget createCellWidget(int colIndex, Bindable target) {
		final BoundWidget widget;
		Field col = this.columns[colIndex];
		if (!wpMap.containsKey(col.getPropertyName())) {
			Property p = GwittirBridge.get().getProperty(target,
					col.getPropertyName());
			BoundWidgetProvider wp = this.factory.getWidgetProvider(col
					.getPropertyName(), p.getType());
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
			widget
					.setValue(p.getAccessorMethod().invoke(target,
							CommonUtils.EMPTY_OBJECT_ARRAY));
		} catch (Exception e) {
		}
		return widget;
	}

	private List selectedObjects = new ArrayList();

	public void setSelectedObjects(List selectedObjects) {
		this.selectedObjects = selectedObjects;
	}

	public List getSelectedObjects() {
		return this.selectedObjects;
	}

	@Override
	protected void addRow(final Bindable o) {
		int row = table.getRowCount();
		final CheckBox handle;
		int startColumn = 0;
		final List<CheckBox> handles = this.rowHandles;
		if ((this.masks & BoundTable.ROW_HANDLE_MASK) > 0) {
			handle = new CheckBox();
			handle.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					setActive(true);
					List newSelected = null;
					if ((masks & BoundTable.MULTIROWSELECT_MASK) > 0) {
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
					}else{
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

	@Override
	protected FlexTable createTableImpl() {
		return new ROFlexTable();
	}

	private static class ROFlexTable extends FlexTable {
		protected boolean internalClearCell(Element td, boolean clearInnerHTML) {
			return false;
		}
	}
}

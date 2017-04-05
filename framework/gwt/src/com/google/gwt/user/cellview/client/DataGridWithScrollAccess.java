/*
 * Copyright 2011 Google Inc.
 * 
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
package com.google.gwt.user.cellview.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * A tabular view with a fixed header and footer section and a scrollable data
 * section in the middle. This widget supports paging and columns.
 * 
 * <p>
 * <h3>Columns</h3> The {@link Column} class defines the
 * {@link com.google.gwt.cell.client.Cell} used to render a column. Implement
 * {@link Column#getValue(Object)} to retrieve the field value from the row
 * object that will be rendered in the {@link com.google.gwt.cell.client.Cell}.
 * </p>
 * 
 * <p>
 * <h3>Headers and Footers</h3> A {@link Header} can be placed at the top
 * (header) or bottom (footer) of the {@link DataGrid}. You can specify a header
 * as text using {@link #addColumn(Column, String)}, or you can create a custom
 * {@link Header} that can change with the value of the cells, such as a column
 * total. The {@link Header} will be rendered every time the row data changes or
 * the table is redrawn. If you pass the same header instance (==) into adjacent
 * columns, the header will span the columns.
 * </p>
 * 
 * <p>
 * <h3>Examples</h3>
 * <dl>
 * <dt>Trivial example</dt>
 * <dd>{@example com.google.gwt.examples.cellview.CellTableExample}</dd>
 * <dt>FieldUpdater example</dt>
 * <dd>{@example com.google.gwt.examples.cellview.CellTableFieldUpdaterExample}
 * </dd>
 * <dt>Key provider example</dt>
 * <dd>{@example com.google.gwt.examples.view.KeyProviderExample}</dd>
 * </dl>
 * </p>
 * 
 * @param <T>
 *            the data type of each row
 */
public class DataGridWithScrollAccess<T> extends DataGrid<T> implements HasDataWidget<T>{
	public DataGridWithScrollAccess(int pageSize,
			com.google.gwt.user.cellview.client.DataGrid.Resources resources) {
		super(pageSize, resources, null,
				createDefaultLoadingIndicator(resources));
	}

	private static Widget createDefaultLoadingIndicator(Resources resources) {
		ImageResource loadingImg = resources.dataGridLoading();
		if (loadingImg == null) {
			return null;
		}
		Image image = new Image(loadingImg);
		image.getElement().getStyle().setMarginTop(30.0, Unit.PX);
		image.setStyleName("dg-loading-image");
		return image;
	}

	public ScrollPanel getBodyScrollPanel() {
		return (ScrollPanel) tableData.getParent();
	}

	@Override
	public void onLoadingStateChanged(LoadingState state) {
		if (state == LoadingState.LOADING && getRowCount() > 0) {
			return;
		}
		super.onLoadingStateChanged(state);
	}
	@Override
	protected void onAttach() {
		// TODO Auto-generated method stub
		super.onAttach();
	}
}

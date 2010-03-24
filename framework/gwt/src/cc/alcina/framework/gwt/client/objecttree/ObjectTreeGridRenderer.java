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
package cc.alcina.framework.gwt.client.objecttree;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

/**
 * 
 * @author Nick Reddel
 */
public class ObjectTreeGridRenderer extends ObjectTreeRenderer {
	protected FlexTable ft = new FlexTable();

	protected Map<Integer, Widget> level1Rows = new LinkedHashMap<Integer, Widget>();

	protected Map<Integer, TreeRenderer> level1RendererRows = new LinkedHashMap<Integer, TreeRenderer>();

	protected FlexCellFormatter cellFormatter;

	protected int colCountMax;

	// a fair bit of hackery here - but, heck, it works
	protected void renderToPanel(TreeRenderable renderable, ComplexPanel cp,
			int depth, boolean soleChild, RenderContext renderContext) {
		super.renderToPanel(renderable, cp, depth, soleChild, renderContext);
		if (depth == 0) {
			cellFormatter = (FlexCellFormatter) ft.getCellFormatter();
			int widgetCount = cp.getWidgetCount();
			colCountMax = 0;
			int level1WidgetIndex = -1;
			int row = -1;
			int col = 0;
			for (int i = 0; i < widgetCount; i++) {
				Widget w = cp.getWidget(0);// we'll be removing widgets, so
				// we'll always be looking at (0)
				if (w.getStyleName().contains("level-1")) {
					level1WidgetIndex = i;
					row++;
					col = 0;
					level1RendererRows.put(row, level1LabelMap.get(w));
				} else {
					colCountMax = Math.max(colCountMax, i - level1WidgetIndex);
				}
				ft.setWidget(row, col, w);
				cellFormatter.setVerticalAlignment(row, col,
						HasVerticalAlignment.ALIGN_BOTTOM);
				if (col == 0) {
					cellFormatter.setStyleName(row, col, "td0");
				}
				boolean isCustomiser = w.getStyleName().contains("customiser");
				if (col == 1) {// && isCustomiser) {
					if (isCustomiser
							&& level1RendererRows.get(row)
									.isSingleLineCustomiser()) {
						cellFormatter.setVerticalAlignment(row, 0,
								HasVerticalAlignment.ALIGN_TOP);
					}
					level1Rows.put(row, w);
				}
				// note, at this point, getWidget(0) is the _next_ widget
				boolean nextWidgetIsNewRow = i == widgetCount - 1
						|| cp.getWidget(0).getStyleName().contains("level-1");
				if (nextWidgetIsNewRow) {
					cellFormatter.setColSpan(row, col, colCountMax - col + 1);
				}
				col++;
			}
			cp.clear();
			cp.add(ft);
		}
		return;
	}
}

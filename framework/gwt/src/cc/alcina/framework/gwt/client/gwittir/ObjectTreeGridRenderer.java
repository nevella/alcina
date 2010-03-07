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

package cc.alcina.framework.gwt.client.gwittir;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class ObjectTreeGridRenderer extends ObjectTreeRenderer {
	private FlexTable ft = new FlexTable();

	// hack, there is a better way of doing this...but TODO: 3.2
	protected void renderToPanel(HasTreeRenderingInfo node, ComplexPanel cp,
			int depth, boolean soleChild) {
		super.renderToPanel(node, cp, depth, soleChild);
		if (depth == 0) {
			FlexCellFormatter cellFormatter = (FlexCellFormatter) ft
					.getCellFormatter();
			int widgetCount = cp.getWidgetCount();
			int colCountMax = 0;
			int level1WidgetIndex = -1;
			int row = -1;
			int col = 0;
			for (int i = 0; i < widgetCount; i++) {
				Widget w = cp.getWidget(0);
				if (w.getStyleName().contains("level-1")) {
					level1WidgetIndex = i;
					row++;
					col = 0;
				} else {
					colCountMax = Math.max(colCountMax, i - level1WidgetIndex);
				}
				ft.setWidget(row, col, w);
				cellFormatter.setVerticalAlignment(row, col,
						HasVerticalAlignment.ALIGN_BOTTOM);
				if (col == 1 && w.getStyleName().contains("customiser")) {
					cellFormatter.setVerticalAlignment(row, 0,
							HasVerticalAlignment.ALIGN_TOP);
				}
				//note, at this point, getWidget(0) is the _next_ widget
				boolean nextWidgetIsNewRow = i == widgetCount - 1
						|| cp.getWidget(0).getStyleName().contains(
								"level-1");
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

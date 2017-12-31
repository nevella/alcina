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
package cc.alcina.framework.gwt.client.gwittir.customiser;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.HasMaxWidth;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class WidthColumnCustomiser implements Customiser {
	public static final String COLUMN_WIDTH = "columnWidth";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		String columnWidth = NamedParameter.Support
				.getParameter(info.parameters(), COLUMN_WIDTH).stringValue();
		return new StringLabelProvider(columnWidth);
	}

	@ClientInstantiable
	public static class StringLabelProvider
			implements BoundWidgetProvider, HasMaxWidth {
		private String columnWidth;

		public StringLabelProvider() {
		}

		public StringLabelProvider(String columnWidth) {
			this.columnWidth = columnWidth;
		}

		public BoundWidget get() {
			return BoundWidgetTypeFactory.LABEL_PROVIDER.get();
		}

		@Override
		public String getColumnWidthString() {
			return columnWidth;
		}

		public int getMaxWidth() {
			return 0;
		}

		@Override
		public int getMinPercentOfTable() {
			return 0;
		}

		public boolean isForceColumnWidth() {
			return false;
		}
	}
}

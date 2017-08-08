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


import com.google.gwt.user.client.ui.FlowPanel;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */

 public class ColourCustomiser implements Customiser {
	public static final String DEFAULT_COLOUR = "defaultColour";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		NamedParameter p = NamedParameter.Support.getParameter(
				info.parameters(), DEFAULT_COLOUR);
		return new ColourRenderer(p==null?null:p.stringValue());
	}

	public static class ColourRenderer implements BoundWidgetProvider {
		private final String defaultColour;

		private static final String DEFAULT_COLOUR = "#FFFFFF";

		public ColourRenderer(String defaultColour) {
			this.defaultColour = defaultColour;
		}

		public BoundWidget get() {
			return new ColourWidget(defaultColour == null ? DEFAULT_COLOUR
					: defaultColour);
		}
	}

	public static class ColourWidget extends
			AbstractBoundWidget< String> {
		private String colour;

		private FlowPanel fp;

		private final String defaultColour;

		public ColourWidget(String defaultColour) {
			this.defaultColour = defaultColour;
			this.fp = new FlowPanel();
			fp.setStyleName("alcina-CustomiserColour");
			updateColour(defaultColour);
			initWidget(fp);
		}

		public String getValue() {
			return colour;
		}

		public void setValue(String value) {
			String old = this.getValue();
			updateColour(value);
			if (this.getValue() != old
					&& (this.getValue() == null || (this.getValue() != null && !this
							.getValue().equals(old)))) {
				this.changes.firePropertyChange("value", old, this.getValue());
			}
		}

		private void updateColour(String value) {
			String colour = value == null ? defaultColour : value;
			fp.getElement().getStyle().setProperty("backgroundColor", colour);
		}
	}
}
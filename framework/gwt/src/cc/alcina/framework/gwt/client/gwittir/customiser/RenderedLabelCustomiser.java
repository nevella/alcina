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
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.ToStringRenderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge.BoundWidgetProviderTextBox;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class RenderedLabelCustomiser implements Customiser {
	public static final String RENDERER_CLASS = "RENDERER_CLASS";

	public static final String WIDGET_CSS_CLASS = "WIDGET_CSS_CLASS";

	public static final String TEXT_BOX_IF_EDITABLE = "TEXT_BOX_IF_EDITABLE";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		Class rendererClass = NamedParameter.Support
				.classValue(info.parameters(), RENDERER_CLASS, null);
		String widgetCssClass = NamedParameter.Support
				.stringValue(info.parameters(), WIDGET_CSS_CLASS, null);
		boolean textBoxIfEditable = NamedParameter.Support
				.booleanValue(info.parameters(), TEXT_BOX_IF_EDITABLE);
		if (editable && textBoxIfEditable) {
			return new BoundWidgetProviderTextBox();
		} else {
			return new RenderedLabelProvider(rendererClass, widgetCssClass);
		}
	}

	public static class RenderedLabelProvider implements BoundWidgetProvider {
		private final String widgetCssClass;

		private Renderer renderer;

		public RenderedLabelProvider(Class rendererClass,
				String widgetCssClass) {
			this.widgetCssClass = widgetCssClass;
			renderer = rendererClass == null ? ToStringRenderer.INSTANCE
					: (Renderer) Reflections.classLookup()
							.newInstance(rendererClass);
		}

		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setRenderer(renderer);
			if (widgetCssClass != null) {
				label.addStyleName(widgetCssClass);
			}
			return label;
		}
	}
}

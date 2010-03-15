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
package cc.alcina.framework.gwt.client.gwittir.customisers;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Label;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

@ClientInstantiable
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */
public class RenderedLabelCustomiser implements Customiser {
	public static final String RENDERER_CLASS = "RENDERER_CLASS";
	public static final String WIDGET_CSS_CLASS = "WIDGET_CSS_CLASS";

	public BoundWidgetProvider getRenderer(boolean editable, Class objectClass,
			boolean multiple, CustomiserInfo info) {
		NamedParameter p = NamedParameter.Support.getParameter(info
				.parameters(), RENDERER_CLASS);
		Class rendererClass= p.classValue();
		p = NamedParameter.Support.getParameter(info.parameters(),
				WIDGET_CSS_CLASS);
		String widgetCssClass = p == null ? null : p.stringValue();
		return new RenderedLabelProvider(rendererClass,widgetCssClass);
	}

	public static class RenderedLabelProvider implements BoundWidgetProvider {
		private final Class rendererClass;
		private final String widgetCssClass;

		public RenderedLabelProvider(Class rendererClass, String widgetCssClass) {
			this.rendererClass = rendererClass;
			this.widgetCssClass = widgetCssClass;
		}

		public BoundWidget get() {
			Label label = new Label();
			label.setRenderer((Renderer) CommonLocator.get().classLookup()
					.newInstance(rendererClass));
			if (widgetCssClass!=null){
				label.addStyleName(widgetCssClass);
			}
			return label;
		}
	}
}

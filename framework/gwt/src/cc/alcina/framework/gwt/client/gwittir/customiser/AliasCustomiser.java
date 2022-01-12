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
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class AliasCustomiser implements Customiser {
	public static final String ALIAS_FIELD = "aliasField";

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		String fieldName = NamedParameter.Support
				.getParameter(info.parameters(), ALIAS_FIELD).stringValue();
		return new AliasProvider(fieldName);
	}

	public static class AliasProvider
			implements BoundWidgetProvider, Renderer<Object, String> {
		private String propertyName;

		private RenderingLabel label;

		public AliasProvider(String propertyName) {
			this.propertyName = propertyName;
		}

		@Override
		public BoundWidget get() {
			label = new RenderingLabel();
			label.setRenderer(this);
			return label;
		}

		@Override
		public String render(Object o) {
			return (String) Reflections.at(label.getModel().getClass())
					.property(propertyName).get(label.getModel());
		}
	}
}
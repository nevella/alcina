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

import java.util.List;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.HasDisplayName.HasDisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;

@ClientInstantiable
@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class ObjectListCustomiser implements Customiser {
	public static final String REGISTRY_POINT = "REGISTRY_POINT";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		NamedParameter[] parameters = info.parameters();
		return new ObjectListWidgetProvider(editable, NamedParameter.Support
				.getParameter(info.parameters(), REGISTRY_POINT).classValue());
	}

	public interface ObjectListProvider<T extends HasDisplayName> {
		public List<T> get();
	}

	public static class ObjectListWidgetProvider
			implements BoundWidgetProvider {
		private final boolean editable;

		private final Class registryPoint;

		public ObjectListWidgetProvider(boolean editable, Class registryPoint) {
			this.editable = editable;
			this.registryPoint = registryPoint;
		}

		public BoundWidget get() {
			if (!editable) {
				RenderingLabel<HasDisplayName> label = new RenderingLabel<HasDisplayName>();
				label.setRenderer(new HasDisplayNameRenderer());
				return label;
			}
			ObjectListProvider listProvider = (ObjectListProvider) Registry
					.impl(registryPoint);
			List options = listProvider.get();
			SetBasedListBox listBox = new SetBasedListBox();
			listBox.setSortOptionsByToString(false);
			listBox.setOptions(options);
			listBox.setRenderer(new HasDisplayNameRenderer());
			return listBox;
		}
	}
}

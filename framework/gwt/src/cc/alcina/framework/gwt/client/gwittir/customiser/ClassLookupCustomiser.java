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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class ClassLookupCustomiser implements Customiser {
	public static final String REGISTRY_POINT = "REGISTRY_POINT";

	public static final String RENDERER_CLASS = "rendererClass";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		NamedParameter[] parameters = info.parameters();
		Renderer renderer = NamedParameter.Support.instantiateClass(parameters,
				RENDERER_CLASS);
		return new RendererClassProvider(editable, NamedParameter.Support
				.getParameter(info.parameters(), REGISTRY_POINT).classValue(),
				renderer);
	}

	public static class ClassShortnameRenderer
			implements Renderer<Class, String> {
		public static final ClassShortnameRenderer INSTANCE = new ClassShortnameRenderer();

		public String render(Class clazz) {
			return clazz == null ? "----" : CommonUtils.simpleClassName(clazz);
		}
	}

	public static class RendererClassProvider implements BoundWidgetProvider {
		private final boolean editable;

		private final Class registryPoint;

		private final Renderer renderer;

		public RendererClassProvider(boolean editable, Class registryPoint,
				Renderer renderer) {
			this.editable = editable;
			this.registryPoint = registryPoint;
			this.renderer = renderer == null ? ClassShortnameRenderer.INSTANCE
					: renderer;
		}

		public BoundWidget get() {
			if (!editable) {
				RenderingLabel<Class> label = new RenderingLabel<Class>();
				label.setRenderer(renderer);
				return label;
			}
			List<Class> lookup = Registry.get().lookup(registryPoint);
			lookup = new ArrayList<Class>(lookup);
			Collections.sort(lookup, new RendererComparator(renderer));
			lookup.add(0, null);
			SetBasedListBox listBox = new SetBasedListBox();
			listBox.setOptions(lookup);
			listBox.setRenderer(renderer);
			return listBox;
		}
	}
}

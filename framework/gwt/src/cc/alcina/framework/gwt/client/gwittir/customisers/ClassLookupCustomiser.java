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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ide.widget.RenderingLabel;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Label;
import com.totsp.gwittir.client.ui.ListBox;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

@ClientInstantiable
@SuppressWarnings("unchecked")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */
public class ClassLookupCustomiser implements Customiser {
	public static final String REGISTRY_POINT = "REGISTRY_POINT";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, CustomiserInfo info) {
		return new RendererClassProvider(editable, NamedParameter.Support
				.getParameter(info.parameters(), REGISTRY_POINT).classValue());
	}

	public static class RendererClassProvider implements BoundWidgetProvider {
		private final boolean editable;

		private final Class registryPoint;

		public RendererClassProvider(boolean editable, Class registryPoint) {
			this.editable = editable;
			this.registryPoint = registryPoint;
		}

		public BoundWidget get() {
			ClassShortnameRenderer renderer = new ClassShortnameRenderer();
			if (!editable) {
				RenderingLabel<Class> label = new RenderingLabel<Class>();
				label.setRenderer(renderer);
				return label;
			}
			List<Class> lookup = Registry.get().lookup(registryPoint);
			lookup = new ArrayList<Class>(lookup);
			Collections.sort(lookup, new RendererComparator(renderer));
			lookup.add(0, null);
			ListBox listBox = new ListBox();
			listBox.setOptions(lookup);
			listBox.setRenderer(renderer);
			return listBox;
		}
	}

	public static class ClassShortnameRenderer implements
			Renderer<Class, String> {
		public String render(Class clazz) {
			return clazz == null ? "----" : CommonUtils.simpleClassName(clazz);
		}
	}

	public static class RendererComparator implements Comparator {
		private final Renderer renderer;

		public RendererComparator(Renderer<?, ? extends Comparable> renderer) {
			this.renderer = renderer;
		}

		public int compare(Object o1, Object o2) {
			return CommonUtils.compareWithNullMinusOne((Comparable) renderer
					.render(o1), (Comparable) renderer.render(o2));
		}
	}
}

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

import java.util.Optional;
import java.util.function.Predicate;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.gwittir.customiser.RenderedLabelCustomiser.RenderedLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ExpandableDomainNodeCollectionLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.SelectorProvider;
import cc.alcina.framework.gwt.client.gwittir.renderer.DisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;

/**
 *
 * 
 *
 */
@Reflected
public class SelectorCustomiser implements Customiser {
	public static final String FILTER_CLASS = "filterClass";

	public static final String MAX_WIDTH = "maxLabelWidth";

	public static final String FORCE_COLUMN_WIDTH = "forceColumnWidth";

	public static final String MAX_SELECTED_ITEMS = "maxSelectedItems";

	public static final String RENDERER_CLASS = "rendererClass";

	public static final String PROVIDER_CLASS = "providerClass";

	public static final String SELECTED_OBJECT_CLASS = "selectedObjectClass";

	public static final String USE_CELL_LIST = "useCellList";

	public static final String USE_MINIMAL_SELECTOR = "useMinimalSelector";

	public static final String USE_FLAT_SELECTOR = "useFlatSelector";

	public static final String HINT = "hint";

	public static final String WITH_NULL = "with-null";

	static final BoundWidgetProvider DN_LABEL_PROVIDER = new BoundWidgetProvider() {
		@Override
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setRenderer(DisplayNameRenderer.INSTANCE);
			return label;
		}
	};

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class clazz,
			boolean multiple, Custom info) {
		if (editable) {
			Predicate filter = null;
			int maxSelectedItems = 0;
			NamedParameter parameter = NamedParameter.Support
					.getParameter(info.parameters(), FILTER_CLASS);
			if (parameter != null) {
				// either populate via indirection, or with an instance of the
				// class itself if there's no registered implementation
				Optional<Predicate> impl = (Optional) Registry
						.optional(parameter.classValue());
				filter = impl.orElse((Predicate) Reflections
						.newInstance(parameter.classValue()));
			}
			Renderer renderer = NamedParameter.Support
					.instantiateClass(info.parameters(), RENDERER_CLASS);
			parameter = NamedParameter.Support.getParameter(info.parameters(),
					MAX_SELECTED_ITEMS);
			if (parameter != null) {
				maxSelectedItems = parameter.intValue();
			}
			parameter = NamedParameter.Support.getParameter(info.parameters(),
					WITH_NULL);
			boolean withNull = parameter == null || parameter.booleanValue();
			boolean useCellList = NamedParameter.Support
					.booleanValue(info.parameters(), USE_CELL_LIST);
			boolean useMinimalSelector = NamedParameter.Support
					.booleanValue(info.parameters(), USE_MINIMAL_SELECTOR);
			boolean useFlatSelector = NamedParameter.Support
					.booleanValue(info.parameters(), USE_FLAT_SELECTOR);
			// implements Supplier<Collection>
			clazz = NamedParameter.Support.classValue(info.parameters(),
					SELECTED_OBJECT_CLASS, clazz);
			Class providerClass = NamedParameter.Support
					.classValue(info.parameters(), PROVIDER_CLASS, null);
			String hint = NamedParameter.Support.stringValue(info.parameters(),
					HINT, null);
			return new SelectorProvider(clazz, filter, maxSelectedItems,
					renderer, useCellList, useMinimalSelector, useFlatSelector,
					hint, providerClass, withNull);
		} else {
			if (multiple) {
				NamedParameter p = NamedParameter.Support
						.getParameter(info.parameters(), MAX_WIDTH);
				int maxLength = p == null
						? BeanFields.MAX_EXPANDABLE_LABEL_LENGTH
						: p.intValue();
				p = NamedParameter.Support.getParameter(info.parameters(),
						FORCE_COLUMN_WIDTH);
				boolean forceColumnWidth = p == null ? true : p.booleanValue();
				return new ExpandableDomainNodeCollectionLabelProvider(
						maxLength, forceColumnWidth);
			} else {
				Class<? extends Renderer> rendererClass = NamedParameter.Support
						.classValue(info.parameters(), RENDERER_CLASS,
								DisplayNameRenderer.class);
				return new RenderedLabelProvider(rendererClass, null);
			}
		}
	}
}

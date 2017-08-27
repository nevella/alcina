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
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxEnumProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;

@ClientInstantiable
@SuppressWarnings("unchecked")
/**
 *
 * @author Nick Reddel
 */
public class EnumCustomiser implements Customiser {
	public static final String MULTIPLE = "multiple";

	public static final String WITH_NULL = "with-null";

	public static final String ENUM_CLASS = "enum-class";

	public static final String ENUM_PROVIDER_CLASS = "enum-provider";

	public static final String RENDERER_CLASS = "renderer-class";

	public static final String HIDDEN_VALUES = "hidden-values";

	public static final String FILTER_CLASS = "filterClass";

	public static final String VISIBLE_ITEM_COUNT = "VISIBLE_ITEM_COUNT";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		NamedParameter parameter = NamedParameter.Support
				.getParameter(info.parameters(), ENUM_CLASS);
		Class<? extends Enum> clazz = parameter.classValue();
		parameter = NamedParameter.Support.getParameter(info.parameters(),
				MULTIPLE);
		boolean multipleSelect = parameter != null && parameter.booleanValue();
		parameter = NamedParameter.Support.getParameter(info.parameters(),
				WITH_NULL);
		boolean withNull = parameter == null || parameter.booleanValue();
		ListBoxEnumProvider provider = new ListBoxEnumProvider(clazz, withNull);
		provider.setMultiple(multipleSelect);
		provider.setVisibleItemCount(NamedParameter.Support
				.intValue(info.parameters(), VISIBLE_ITEM_COUNT, 4));
		parameter = NamedParameter.Support.getParameter(info.parameters(),
				RENDERER_CLASS);
		final Renderer renderer = parameter != null ? (Renderer) Reflections
				.classLookup().newInstance(parameter.classValue()) : null;
		if (renderer != null) {
			provider.setRenderer(renderer);
		}
		String hiddenValuesStr = NamedParameter.Support
				.stringValue(info.parameters(), HIDDEN_VALUES, null);
		if (hiddenValuesStr != null) {
			List<Enum> hiddenValues = new ArrayList<Enum>();
			String[] enumStrs = hiddenValuesStr.split(",");
			for (String enumStr : enumStrs) {
				Enum[] enumConstants = clazz.getEnumConstants();
				for (Enum en : enumConstants) {
					if (en.toString().equals(enumStr)) {
						hiddenValues.add(en);
					}
				}
			}
			provider.setHiddenValues(hiddenValues);
		}
		parameter = NamedParameter.Support.getParameter(info.parameters(),
				ENUM_PROVIDER_CLASS);
		if (parameter != null) {
			ArrayList hiddenValues = new ArrayList(EnumSet.allOf(clazz));
			hiddenValues
					.removeAll(((HasValue<Collection>) Reflections.classLookup()
							.newInstance(parameter.classValue())).getValue());
			provider.setHiddenValues(hiddenValues);
		}
		parameter = NamedParameter.Support.getParameter(info.parameters(),
				FILTER_CLASS);
		if (parameter != null) {
			ArrayList hiddenValues = new ArrayList(EnumSet.allOf(clazz));
			hiddenValues.removeAll(CollectionFilters.filter(hiddenValues,
					(CollectionFilter) Reflections.classLookup()
							.newInstance(parameter.classValue())));
			provider.setHiddenValues(hiddenValues);
		}
		return editable ? provider : new BoundWidgetProvider() {
			public BoundWidget get() {
				RenderingLabel<Enum> label = new RenderingLabel<Enum>();
				if (renderer != null) {
					label.setRenderer(renderer);
				}
				label.setWordWrap(false);
				return label;
			}
		};
	}
}

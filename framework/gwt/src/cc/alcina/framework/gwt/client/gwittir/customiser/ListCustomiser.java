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

import java.util.Comparator;

import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.provider.ExpandableDomainNodeCollectionLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.ListBoxCollectionProvider;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
@ClientInstantiable
public class ListCustomiser implements Customiser {
	public static final String FILTER_CLASS = "filterClass";

	public static final String RENDERER_CLASS = "rendererClass";

	public static final String COMPARATOR_CLASS = "comparatorClass";

	public static final String MAX_WIDTH = "maxLabelWidth";

	public static final String FORCE_COLUMN_WIDTH = "forceColumnWidth";

	public static final String MAX_SELECTED_ITEMS = "maxSelectedItems";

	public static final String NO_NULL = "noNull";

	public static final String ADD_HANDLER_CLASS = "addHandlerClass";
	
	public static final String REFRESH_ON_MODEL_CHANGE = "refreshOnModelChange";

	
	public BoundWidgetProvider getProvider(boolean editable, Class clazz,
			boolean multiple, Custom info) {
		NamedParameter[] parameters = info.parameters();
		if (editable) {
			CollectionFilter filter = NamedParameter.Support
					.instantiateClass(parameters, FILTER_CLASS);
			Renderer renderer = NamedParameter.Support
					.instantiateClass(parameters, RENDERER_CLASS);
			Comparator comparator = NamedParameter.Support
					.instantiateClass(parameters, COMPARATOR_CLASS);
			ListAddItemHandler addHandler = NamedParameter.Support
					.instantiateClass(parameters, ADD_HANDLER_CLASS);
			int maxSelectedItems = NamedParameter.Support.intValue(parameters,
					MAX_SELECTED_ITEMS, 1);
			boolean nonull = NamedParameter.Support.booleanValue(parameters,
					NO_NULL);
			boolean refreshOnModelChange = NamedParameter.Support.booleanValue(parameters,
			        REFRESH_ON_MODEL_CHANGE);
			ListBoxCollectionProvider lbcp = new ListBoxCollectionProvider(
					clazz, maxSelectedItems != 1, nonull, renderer, comparator,
					addHandler);
			lbcp.setFilter(filter);
			lbcp.setRefreshOnModelChange(refreshOnModelChange);
			return lbcp;
		} else {
			if (multiple) {
				NamedParameter p = NamedParameter.Support
						.getParameter(parameters, MAX_WIDTH);
				int maxLength = p == null
						? GwittirBridge.MAX_EXPANDABLE_LABEL_LENGTH
						: p.intValue();
				p = NamedParameter.Support.getParameter(parameters,
						FORCE_COLUMN_WIDTH);
				boolean forceColumnWidth = p == null ? true : p.booleanValue();
				return new ExpandableDomainNodeCollectionLabelProvider(
						maxLength, forceColumnWidth);
			} else {
				return GwittirBridge.DN_LABEL_PROVIDER;
			}
		}
	}
}

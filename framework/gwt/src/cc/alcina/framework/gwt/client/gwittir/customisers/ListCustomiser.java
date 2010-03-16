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
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge.ExpandableDomainNodeCollectionLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge.ListBoxCollectionProvider;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;

import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
@ClientInstantiable
public class ListCustomiser implements Customiser {
	public static final String FILTER_CLASS = "filterClass";

	public static final String MAX_WIDTH = "maxLabelWidth";

	public static final String FORCE_COLUMN_WIDTH = "forceColumnWidth";

	public static final String MAX_SELECTED_ITEMS = "maxSelectedItems";

	public static final String NO_NULL = "noNull";
	@SuppressWarnings("unchecked")
	public BoundWidgetProvider getProvider(boolean editable, Class clazz,
			boolean multiple, CustomiserInfo info) {
		if (editable) {
			CollectionFilter filter = null;
			int maxSelectedItems = 1;
			NamedParameter parameter = NamedParameter.Support.getParameter(info
					.parameters(), FILTER_CLASS);
			if (parameter != null) {
				filter = (CollectionFilter) CommonLocator.get().classLookup()
						.newInstance(parameter.classValue(), 0);
			}
			parameter = NamedParameter.Support.getParameter(info.parameters(),
					MAX_SELECTED_ITEMS);
			if (parameter != null) {
				maxSelectedItems = parameter.intValue();
			}
			parameter = NamedParameter.Support.getParameter(info.parameters(),
					NO_NULL);
			boolean nonull = parameter != null;
			ListBoxCollectionProvider lbcp = new ListBoxCollectionProvider(
					clazz, maxSelectedItems != 1, nonull);
			lbcp.setFilter(filter);
			return lbcp;
		} else {
			if (multiple) {
				NamedParameter p = NamedParameter.Support.getParameter(info
						.parameters(), MAX_WIDTH);
				int maxLength = p == null ? GwittirBridge.MAX_EXPANDABLE_LABEL_LENGTH
						: p.intValue();
				p = NamedParameter.Support.getParameter(info.parameters(),
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

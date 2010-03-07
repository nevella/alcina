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

package cc.alcina.framework.gwt.client.gwittir;


import cc.alcina.framework.gwt.client.gwittir.customisers.SelectorCustomiser.BoundSelector;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;

import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class SelectorProvider implements BoundWidgetProvider {
	private final Class selectionObjectClass;
	private final CollectionFilter filter;
	private final int maxSelectedItems;

	public SelectorProvider(Class selectionObjectClass, CollectionFilter filter, int maxSelectedItems) {
		this.selectionObjectClass = selectionObjectClass;
		this.filter = filter;
		this.maxSelectedItems = maxSelectedItems;
	}

	public BoundSelector get() {
		return new BoundSelector(selectionObjectClass, filter,maxSelectedItems);
	}
}
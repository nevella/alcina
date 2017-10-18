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
package cc.alcina.framework.gwt.client.gwittir.provider;

import java.util.Collection;
import java.util.function.Supplier;

import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelector;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelectorMinimal;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;

/**
 *
 * @author Nick Reddel
 */
public class SelectorProvider implements BoundWidgetProvider {
	private final Class selectionObjectClass;

	private final CollectionFilter filter;

	private final int maxSelectedItems;

	private final Renderer renderer;

	private boolean useCellList;

	private boolean useMinimalSelector;

	private boolean useFlatSelector;

	private String hint;

	public SelectorProvider(Class selectionObjectClass, CollectionFilter filter,
			int maxSelectedItems, Renderer renderer, boolean useCellList,
			boolean useMinimalSelector, boolean useFlatSelector, String hint) {
		this.selectionObjectClass = selectionObjectClass;
		this.filter = filter;
		this.maxSelectedItems = maxSelectedItems;
		this.renderer = renderer;
		this.useCellList = useCellList;
		this.useMinimalSelector = useMinimalSelector;
		this.useFlatSelector = useFlatSelector;
		this.hint = hint;
	}

	public BoundSelector get() {
		if (useFlatSelector) {
			return new FlatSearchSelector(selectionObjectClass,
					maxSelectedItems, renderer, new Supplier<Collection>() {
						@Override
						public Collection get() {
							return TransformManager.get()
									.getCollection(selectionObjectClass);
						}
					});
		} else if (useMinimalSelector) {
			BoundSelectorMinimal selectorMinimal = new BoundSelectorMinimal(
					selectionObjectClass, filter, maxSelectedItems, renderer,
					hint);
			return selectorMinimal;
		} else {
			return new BoundSelector(selectionObjectClass, filter,
					maxSelectedItems, renderer, useCellList);
		}
	}
}
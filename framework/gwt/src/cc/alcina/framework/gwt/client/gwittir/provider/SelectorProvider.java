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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelector;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelectorMinimal;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;

/**
 *
 * @author Nick Reddel
 */
public class SelectorProvider implements BoundWidgetProvider {
	private final Class selectionObjectClass;

	private final Predicate filter;

	private final int maxSelectedItems;

	private final Renderer renderer;

	private boolean useCellList;

	private boolean useMinimalSelector;

	private boolean useFlatSelector;

	private String hint;

	private Class providerClass;

	private boolean withNull;

	public SelectorProvider(Class selectionObjectClass, Predicate filter,
			int maxSelectedItems, Renderer renderer, boolean useCellList,
			boolean useMinimalSelector, boolean useFlatSelector, String hint,
			Class providerClass, boolean withNull) {
		this.selectionObjectClass = selectionObjectClass;
		this.filter = filter;
		this.maxSelectedItems = maxSelectedItems;
		this.renderer = renderer;
		this.useCellList = useCellList;
		this.useMinimalSelector = useMinimalSelector;
		this.useFlatSelector = useFlatSelector;
		this.hint = hint;
		this.providerClass = providerClass;
		this.withNull = withNull;
	}

	@Override
	public BoundSelector get() {
		if (useFlatSelector) {
			Supplier<Collection> provider = new Supplier<Collection>() {
				@Override
				public Collection get() {
					if (selectionObjectClass.isEnum()) {
						List<Object> values = Arrays
								.asList(selectionObjectClass.getEnumConstants())
								.stream().collect(Collectors.toList());
						if (withNull) {
							values.add(0, null);
						}
						return values;
					} else {
						List values = (List) TransformManager.get()
								.getCollection(selectionObjectClass).stream()
								.collect(Collectors.toList());
						if (withNull) {
							values.add(0, null);
						}
						return values;
					}
				}
			};
			if (providerClass != null) {
				provider = (Supplier<Collection>) Reflections
						.newInstance(providerClass);
			}
			return new FlatSearchSelector(selectionObjectClass,
					maxSelectedItems, renderer, provider);
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
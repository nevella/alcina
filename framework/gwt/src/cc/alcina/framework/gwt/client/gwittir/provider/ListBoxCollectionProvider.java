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

import java.util.Comparator;

import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.gwt.client.gwittir.Comparators;
import cc.alcina.framework.gwt.client.gwittir.customiser.ListAddItemHandler;
import cc.alcina.framework.gwt.client.gwittir.renderer.DisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox.DomainListBox;

public class ListBoxCollectionProvider implements BoundWidgetProvider {
	private final Class clazz;

	private final boolean propertyIsCollection;

	private final boolean noNullOption;

	private CollectionFilter filter;

	private Renderer renderer;

	private Comparator comparator;

	private final ListAddItemHandler addHandler;
	
	private boolean refreshOnModelChange;

	public ListBoxCollectionProvider(Class clazz,
			boolean propertyIsCollection) {
		this(clazz, propertyIsCollection, false);
	}

    public ListBoxCollectionProvider(Class clazz, boolean propertyIsCollection,
			boolean noNullOption) {
		this(clazz, propertyIsCollection, noNullOption, null, null, null);
	}

    public ListBoxCollectionProvider(Class clazz, boolean propertyIsCollection,
			boolean noNullOption, Renderer renderer, Comparator comparator) {
		this(clazz, propertyIsCollection, noNullOption, renderer, comparator,
				null);
	}

	public ListBoxCollectionProvider(Class clazz, boolean propertyIsCollection,
			boolean noNullOption, Renderer renderer, Comparator comparator,
			ListAddItemHandler addHandler) {
		this.clazz = clazz;
		this.propertyIsCollection = propertyIsCollection;
		this.noNullOption = noNullOption;
		this.renderer = renderer;
		this.comparator = comparator;
		this.addHandler = addHandler;
	}

	public DomainListBox get() {
		DomainListBox listBox = new DomainListBox(clazz, filter,
				!propertyIsCollection && !noNullOption, addHandler);
		listBox.setRenderer(
				renderer == null ? DisplayNameRenderer.INSTANCE : renderer);
		listBox.setComparator(comparator == null
				? Comparators.EqualsComparator.INSTANCE : comparator);
		listBox.setSortOptionsByToString(comparator == null);
		listBox.setMultipleSelect(propertyIsCollection);
		listBox.setRefreshOnModelChange(refreshOnModelChange);
		return listBox;
	}

	public CollectionFilter getFilter() {
		return filter;
	}

	public boolean isRefreshOnModelChange() {
        return this.refreshOnModelChange;
    }

	public void setFilter(CollectionFilter filter) {
		this.filter = filter;
	}

	public void setRefreshOnModelChange(boolean refreshOnModelChange) {
        this.refreshOnModelChange = refreshOnModelChange;
    }
}
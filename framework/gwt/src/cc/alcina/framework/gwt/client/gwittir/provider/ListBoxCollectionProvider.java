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

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.gwt.client.gwittir.Comparators;
import cc.alcina.framework.gwt.client.gwittir.renderer.DisplayNameRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox.DomainListBox;

import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

public  class ListBoxCollectionProvider implements
		BoundWidgetProvider {
	private final Class clazz;

	private final boolean propertyIsCollection;

	private final boolean noNullOption;

	private CollectionFilter filter;

	private Renderer renderer;

	private  Comparator comparator;

	public ListBoxCollectionProvider(Class clazz,
			boolean propertyIsCollection) {
		this(clazz, propertyIsCollection, false);
	}
	public ListBoxCollectionProvider(Class clazz,
			boolean propertyIsCollection, boolean noNullOption){
		this(clazz, propertyIsCollection, noNullOption, null,null);
	}
	public ListBoxCollectionProvider(Class clazz,
			boolean propertyIsCollection, boolean noNullOption,
			Renderer renderer, Comparator comparator) {
		this.clazz = clazz;
		this.propertyIsCollection = propertyIsCollection;
		this.noNullOption = noNullOption;
		this.renderer = renderer;
		this.comparator = comparator;
	}

	public DomainListBox get() {
		DomainListBox listBox = new DomainListBox(clazz, filter,
				!propertyIsCollection && !noNullOption);
		listBox.setRenderer(renderer == null ? DisplayNameRenderer.INSTANCE
				: renderer);
		listBox.setComparator(comparator==null?Comparators.EqualsComparator.INSTANCE:comparator);
		listBox.setSortOptionsByToString(comparator==null);
		listBox.setMultipleSelect(propertyIsCollection);
		return listBox;
	}

	public void setFilter(CollectionFilter filter) {
		this.filter = filter;
	}

	public CollectionFilter getFilter() {
		return filter;
	}
}
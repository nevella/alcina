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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.gwt.client.gwittir.Comparators;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.SetBasedListBox;

/**
 *
 * @author Nick Reddel
 */
public class ListBoxEnumProvider implements BoundWidgetProvider {
	private final Class<? extends Enum> clazz;

	private List<Enum> hiddenValues = new ArrayList<Enum>();

	private boolean withNull;

	private boolean multiple;

	private int visibleItemCount = 4;

	private Renderer renderer = FriendlyEnumRenderer.INSTANCE;

	private HasValue<Collection> provider;

	public ListBoxEnumProvider(Class<? extends Enum> clazz) {
		this(clazz, false);
	}

	public ListBoxEnumProvider(Class<? extends Enum> clazz, boolean withNull) {
		this.clazz = clazz;
		this.setWithNull(withNull);
	}

	public SetBasedListBox get() {
		SetBasedListBox listBox = new SetBasedListBox();
		Enum[] enumValues = clazz.getEnumConstants();
		List options = new ArrayList(Arrays.asList(enumValues));
		for (Enum e : hiddenValues) {
			options.remove(e);
		}
		if (provider != null) {
			options = (List) provider.getValue();
		}
		if (isWithNull()) {
			options.add(0, null);
		}
		listBox.setRenderer(getRenderer());
		listBox.setComparator(Comparators.EqualsComparator.INSTANCE);
		listBox.setSortOptionsByToString(false);
		listBox.setOptions(options);
		listBox.setMultipleSelect(multiple);
		if (multiple) {
			listBox.setVisibleItemCount(visibleItemCount);
		}
		return listBox;
	}

	public Class<? extends Enum> getEnumClass() {
		return clazz;
	}

	public List<Enum> getHiddenValues() {
		return hiddenValues;
	}

	public Renderer getRenderer() {
		return renderer;
	}

	public int getVisibleItemCount() {
		return this.visibleItemCount;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public boolean isWithNull() {
		return withNull;
	}

	public void setHiddenValues(List<Enum> hiddenValues) {
		this.hiddenValues = hiddenValues;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	public void setVisibleItemCount(int visibleItemCount) {
		this.visibleItemCount = visibleItemCount;
	}

	public void setWithNull(boolean withNull) {
		this.withNull = withNull;
	}

	public void setProvider(HasValue<Collection> provider) {
		this.provider = provider;
	}
}
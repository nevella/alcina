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
package cc.alcina.framework.gwt.client.gwittir.widget;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;

@SuppressWarnings("deprecation")
/**
 *
 * @author Nick Reddel
 */
public class RadioButtonList<T> extends AbstractBoundWidget<T> implements
		ClickHandler {
	Map<String, T> labelMap = new HashMap<String, T>();

	Map<T, RadioButton> radioMap = new HashMap<T, RadioButton>();

	private FlowPanel fp;

	private Collection<T> values;

	private final Renderer<T, String> renderer;

	private final String groupName;

	private int columnCount = 1;

	private T lastValue;

	private T nonMatchedValue;

	private FlexTable table;

	public RadioButtonList(String groupName, Collection<T> values,
			Renderer<T, String> renderer) {
		this.groupName = groupName;
		this.renderer = renderer;
		this.fp = new FlowPanel();
		this.setValues(values);
		initWidget(fp);
	}

	public RadioButtonList(String groupName, Collection<T> values,
			Renderer<T, String> renderer, int columnCount) {
		this(groupName, values, renderer);
		setColumnCount(columnCount);
	}

	public int getColumnCount() {
		return columnCount;
	}

	public T getValue() {
		Set<T> keySet = radioMap.keySet();
		for (T object : keySet) {
			if (radioMap.get(object).isChecked()) {
				return object;
			}
		}
		return nonMatchedValue;
	}

	public Collection<T> getValues() {
		return values;
	}

	public boolean hasSelectedButton() {
		Set<T> keySet = radioMap.keySet();
		for (T object : keySet) {
			if (radioMap.get(object).isChecked()) {
				return true;
			}
		}
		return false;
	}

	public void setColumnCount(int width) {
		this.columnCount = width;
		render();
	}

	public void setValue(T value) {
		Set<T> keySet = radioMap.keySet();
		boolean matched = false;
		for (T object : keySet) {
			boolean cMatched = object.equals(value);
			radioMap.get(object).setChecked(cMatched);
			matched |= cMatched;
		}
		nonMatchedValue = matched ? null : value;
		if (!CommonUtils.equalsWithNullEquality(value, lastValue)) {
			this.changes
					.firePropertyChange("value", lastValue, this.getValue());
		}
		lastValue = value;
	}

	public void setValues(Collection<T> values) {
		this.values = values;
		render();
		if (lastValue != null) {
			setValue(lastValue);
		}
	}

	private void render() {
		fp.clear();
		radioMap.clear();
		table=new FlexTable();
		int x = 0, y = 0;
		for (T o : getValues()) {
			String displayText = renderer.render(o);
			labelMap.put(displayText, o);
			RadioButton rb = new RadioButton(groupName, displayText);
			radioMap.put(o, rb);
			table.setWidget(y, x++, rb);
			if (x == getColumnCount()) {
				x = 0;
				y++;
			}
			rb.addClickHandler(this);
		}
		fp.add(table);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onClick(ClickEvent event) {
		Set<T> keySet = radioMap.keySet();
		for (T object : keySet) {
			if (radioMap.get(object).equals(event.getSource())) {
				setValue((T) object);
			}
		}
	}

	public FlexTable getTable() {
		return this.table;
	}
}

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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.totsp.gwittir.client.ui.AbstractBoundCollectionWidget;
import com.totsp.gwittir.client.ui.Renderer;

import cc.alcina.framework.common.client.util.CommonUtils;

@SuppressWarnings("deprecation")
/**
 *
 * @author Nick Reddel
 *
 */
public class RadioButtonList<T> extends AbstractBoundCollectionWidget implements
		ClickHandler {
	Map<String, T> labelMap = new HashMap<String, T>();

	Map<T, CheckBox> checkMap = new HashMap<T, CheckBox>();

	private FlowPanel fp;

	private Collection<T> values;

	private Renderer<T, String> renderer;

	private String groupName;

	private Renderer<T, ImageResource> iconRenderer;

	private int columnCount = 1;

	private Collection lastValues;

	private Object nonMatchedValue;

	private FlexTable table;

	public RadioButtonList() {
	}

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

	public RadioButtonList(String groupName, Object[] values,
			Renderer<T, String> renderer, int columnCount) {
		this(groupName, (Collection<T>) Arrays.asList(values), renderer,
				columnCount);
	}

	public int getColumnCount() {
		return columnCount;
	}

	protected boolean singleResult() {
		return true;
	}

	public Object getValue() {
		Set<T> keySet = checkMap.keySet();
		Set results = new LinkedHashSet();
		for (T object : keySet) {
			if (checkMap.get(object).getValue()) {
				results.add(object);
			}
		}
		if (results.isEmpty() && singleResult()) {
			if (nonMatchedValue instanceof Collection
					&& ((Collection) nonMatchedValue).isEmpty()) {
			} else {
				if (nonMatchedValue != null) {
					results.add(nonMatchedValue);
				}
			}
		}
		return singleResult() ? singleValue(results) : results;
	}

	public Collection<T> getValues() {
		return values;
	}

	public boolean hasSelectedButton() {
		Set<T> keySet = checkMap.keySet();
		for (T object : keySet) {
			if (checkMap.get(object).isChecked()) {
				return true;
			}
		}
		return false;
	}

	public void setColumnCount(int width) {
		this.columnCount = width;
		render();
	}

	/*
	 * treat the comparison etc as always set based (even if actually
	 * single-object) to simplify code
	 */
	public void setValue(Object value) {
		Collection values = CommonUtils.wrapInCollection(value);
		values = values == null ? new LinkedHashSet() : values;
		Set<T> keySet = checkMap.keySet();
		boolean matched = false;
		for (T object : keySet) {
			boolean cMatched = values.contains(object);
			checkMap.get(object).setValue(cMatched);
			matched |= cMatched;
		}
		nonMatchedValue = matched ? null : value;
		/*
		 * i.e. a value not in the // checkbox list - keep it // as a return
		 * value for // getValue (rather than null)
		 */
		if (!CommonUtils.equalsWithNullEmptyEquality(value, lastValues)) {
			this.changes.firePropertyChange("value",
					singleResult() ? singleValue((Collection<T>) lastValues)
							: lastValues, getValue());
		}
		lastValues = values;
	}

	public void setValues(Collection<T> values) {
		this.values = values;
		render();
		if (lastValues != null) {
			setValue(lastValues);
		}
	}

	private void render() {
		fp.clear();
		checkMap.clear();
		table = new FlexTable();
		int x = 0, y = 0;
		for (T o : getValues()) {
			String displayText = renderer.render(o);
			labelMap.put(displayText, o);
			if (iconRenderer != null) {
				String imgHtml = AbstractImagePrototype.create(
						iconRenderer.render(o)).getHTML();
				displayText = CommonUtils
						.formatJ(
								"<span class='radio-button-icon'>%s</span><span class='radio-button-icon-label'>%s</span>",
								imgHtml, displayText);
			}
			CheckBox rb = createCheckBox(displayText);
			checkMap.put(o, rb);
			table.setWidget(y, x++, rb);
			if (x == getColumnCount()) {
				x = 0;
				y++;
			}
			rb.addClickHandler(this);
		}
		fp.add(table);
	}

	protected CheckBox createCheckBox(String displayText) {
		return new RadioButton(groupName, displayText, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onClick(ClickEvent event) {
		Set<T> keySet = checkMap.keySet();
		for (T object : keySet) {
			if (checkMap.get(object).equals(event.getSource())) {
				if (singleResult()) {
					setValue((T) object);
				} else {
					Collection c = (Collection) getValue();
					setValue(c);
				}
				break;
			}
		}
	}

	public FlexTable getTable() {
		return this.table;
	}

	public Renderer<T, ImageResource> getIconRenderer() {
		return this.iconRenderer;
	}

	public void setIconRenderer(Renderer<T, ImageResource> iconRenderer) {
		this.iconRenderer = iconRenderer;
		render();
	}

	private T singleValue(Collection<T> values) {
		return CommonUtils.isNullOrEmpty(values) ? null : values.iterator()
				.next();
	}

	public T singleValue() {
		return (T) (getValue() instanceof Collection ? singleValue((Collection<T>) getValue())
				: getValue());
	}
}

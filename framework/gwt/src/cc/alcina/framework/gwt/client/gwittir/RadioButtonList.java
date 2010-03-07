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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
@SuppressWarnings("deprecation")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class RadioButtonList<B, V> extends AbstractBoundWidget<B, V> implements
		ClickListener {
	Map<String, V> labelMap = new HashMap<String, V>();

	Map<V, RadioButton> radioMap = new HashMap<V, RadioButton>();

	private FlowPanel fp;

	private final Collection<V> values;

	private final Renderer<String, V> renderer;

	private final String groupName;

	private int columnCount = 1;
	
	private void render() {
		fp.clear();
		Grid grid = new Grid((int) Math.ceil((double) values.size()
				/ (double) getColumnCount()), getColumnCount());
		int x=0, y=0;
		for (V o : values) {
			String displayText = renderer.render(o);
			labelMap.put(displayText, o);
			RadioButton rb = new RadioButton(groupName, displayText);
			radioMap.put(o, rb);
			grid.setWidget(y, x++, rb);
			if (x==getColumnCount()){
				x=0;
				y++;
			}
			rb.addClickListener(this);
		}
		fp.add(grid);
	}

	public RadioButtonList(String groupName, Collection<V> values,
			Renderer<String, V> renderer) {
		this.groupName = groupName;
		this.values = values;
		this.renderer = renderer;
		this.fp = new FlowPanel();
		render();
		initWidget(fp);
	}

	public V getValue() {
		Set<V> keySet = radioMap.keySet();
		for (V object : keySet) {
			if (radioMap.get(object).isChecked()) {
				return object;
			}
		}
		return null;
	}

	private B lastValue;

	public void setValue(B value) {
		Set<V> keySet = radioMap.keySet();
		for (V object : keySet) {
			radioMap.get(object).setChecked(object.equals(value));
		}
		if ( value != lastValue) {
			this.changes
					.firePropertyChange("value", lastValue, this.getValue());
		}
		lastValue = value;
	}
	@SuppressWarnings("unchecked")
	public void onClick(Widget sender) {
		Set<V> keySet = radioMap.keySet();
		for (V object : keySet) {
			if (radioMap.get(object).equals(sender)) {
				setValue((B) object);
			}
		}
	}

	public void setColumnCount(int width) {
		this.columnCount = width;
		render();
	}

	public int getColumnCount() {
		return columnCount;
	}
}

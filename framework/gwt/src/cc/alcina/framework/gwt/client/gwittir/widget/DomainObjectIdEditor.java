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

import java.util.Date;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.renderer.IdToStringRenderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * No validation (yet) - this is definitely an admin-only widget. Will throw
 * transform errors if an invalid ID is set, too...
 * 
 * @author Nick Reddel
 */
public class DomainObjectIdEditor extends AbstractBoundWidget implements
		ValueChangeHandler {
	public static class DomainObjectIdEditorProvider implements
			BoundWidgetProvider {
		private final Class domainObjectClass;

		public DomainObjectIdEditorProvider(Class domainObjectClass) {
			this.domainObjectClass = domainObjectClass;
		}

		public BoundWidget get() {
			DomainObjectIdEditor editor = new DomainObjectIdEditor(
					domainObjectClass);
			return editor;
		}
	};

	private Class<? extends HasIdAndLocalId> domainObjectClass;

	private FlowPanel fp;

	private TextBox tb;

	public DomainObjectIdEditor() {
	}

	public DomainObjectIdEditor(Class domainObjectClass) {
		this.domainObjectClass = domainObjectClass;
		fp = new FlowPanel();
		this.tb = new TextBox();
		tb.addValueChangeHandler(this);
		fp.add(tb);
		initWidget(fp);
	}

	private Object currentValue;

	public Object getValue() {
		String text = tb.getText();
		Long id = null;
		if (!CommonUtils.isNullOrEmpty(text)) {
			if (text.equals("null")) {
				currentValue = null;
			} else {
				try {
					id = Long.parseLong(text);
					HasIdAndLocalId hili = CommonLocator.get().classLookup()
							.newInstance(domainObjectClass);
					hili.setId(id);
					if (hili != null && !hili.equals(currentValue)
							&& TransformManager.get().getObject(hili) == null) {
						TransformManager.get().registerDomainObject(hili);
					}
					currentValue = hili;
				} catch (Exception e) {
				}
			}
		}
		return currentValue;
	}

	public void setValue(Object value) {
		Object old = getValue();
		currentValue = value;
		if (CommonUtils.isNullOrEmpty(tb.getText())) {
			tb.setText(IdToStringRenderer.INSTANCE
					.render((HasIdAndLocalId) currentValue));
		}
		changes.firePropertyChange("value", old, getValue());
	}

	public void onValueChange(ValueChangeEvent event) {
		changes.firePropertyChange("value", currentValue, getValue());
	}
}

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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.renderer.IdToStringRenderer;

/**
 * No validation (yet) - this is definitely an admin-only widget. Will throw
 * transform errors if an invalid ID is set, too...
 * 
 * @author Nick Reddel
 */
public class DomainObjectIdEditor extends AbstractBoundWidget
		implements ValueChangeHandler {
	private Class<? extends Entity> domainObjectClass;

	private FlowPanel fp;

	private TextBox tb;

	private Object currentValue;

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

	@Override
	public Object getValue() {
		String text = tb.getText();
		Long id = null;
		if (!CommonUtils.isNullOrEmpty(text)) {
			if (text.equals("null")) {
				currentValue = null;
			} else {
				try {
					id = Long.parseLong(text);
					Entity entity = Reflections.classLookup()
							.newInstance(domainObjectClass);
					entity.setId(id);
					if (entity != null && !entity.equals(currentValue)
							&& TransformManager.get()
									.getObject(entity) == null) {
						TransformManager.get().registerDomainObject(entity);
					}
					currentValue = entity;
				} catch (Exception e) {
				}
			}
		}
		return currentValue;
	}

	@Override
	public void onValueChange(ValueChangeEvent event) {
		changes.firePropertyChange("value", currentValue, getValue());
	}

	@Override
	public void setValue(Object value) {
		Object old = getValue();
		currentValue = value;
		if (CommonUtils.isNullOrEmpty(tb.getText())) {
			tb.setText(IdToStringRenderer.BLANK_NULLS_INSTANCE
					.render((Entity) currentValue));
		}
		changes.firePropertyChange("value", old, getValue());
	}

	public static class DomainObjectIdEditorProvider
			implements BoundWidgetProvider {
		private final Class domainObjectClass;

		public DomainObjectIdEditorProvider(Class domainObjectClass) {
			this.domainObjectClass = domainObjectClass;
		}

		@Override
		public BoundWidget get() {
			DomainObjectIdEditor editor = new DomainObjectIdEditor(
					domainObjectClass);
			return editor;
		}
	}
}

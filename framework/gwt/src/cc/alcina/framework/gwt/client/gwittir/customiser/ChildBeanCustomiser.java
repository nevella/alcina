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
package cc.alcina.framework.gwt.client.gwittir.customiser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.entity.GeneralProperties;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.gwittir.HasBinding;
import cc.alcina.framework.gwt.client.gwittir.widget.GridForm;
import cc.alcina.framework.gwt.client.gwittir.widget.GridFormCellRendererGrid;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.RecheckVisibilityHandler;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.Link;

@Reflected
/**
 *
 * @author Nick Reddel
 */
public class ChildBeanCustomiser implements Customiser {
	public static final String EXCLUDE_FIELDS = "EXCLUDE_FIELDS";

	@Override
	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		String excludesStr = NamedParameter.Support
				.stringValue(info.parameters(), EXCLUDE_FIELDS, null);
		return new ChildBeanRenderer(editable, objectClass, excludesStr);
	}

	public static class ChildBeanRenderer implements BoundWidgetProvider {
		private final boolean editable;

		private final Class objectClass;

		private String excludesStr;

		public ChildBeanRenderer(boolean editable, Class objectClass,
				String excludesStr) {
			this.editable = editable;
			this.objectClass = objectClass;
			this.excludesStr = excludesStr;
		}

		@Override
		public BoundWidget get() {
			return new ChildBeanWidget(objectClass, editable, excludesStr);
		}
	}

	public static class ChildBeanWidget extends AbstractBoundWidget
			implements MultilineWidget, HasBinding {
		private FlowPanel fp;

		private GridForm gridForm;

		private FlowPanel createPanel;

		private Class objectClass;

		public ChildBeanWidget() {
		}

		public ChildBeanWidget(Class objectClass, final boolean editable,
				String excludesStr) {
			this.objectClass = objectClass;
			List<String> excludeList = excludesStr == null ? new ArrayList<>()
					: Arrays.asList(excludesStr.split(","));
			Predicate<String> filter = n -> !excludeList.contains(n);
			List<Field> fields = BeanFields.query().forClass(objectClass)
					.withEditable(editable).withEditableNamePredicate(filter)
					.listFields();
			this.gridForm = new GridForm(fields, 1,
					new GridFormCellRendererGrid(false));
			gridForm.setDirectSetModelDisabled(true);
			gridForm.addAttachHandler(new RecheckVisibilityHandler(gridForm));
			// the model should be the child
			// bean(value), not the parent
			gridForm.setStyleName("alcina-ChildBean");
			this.fp = new FlowPanel();
			gridForm.setVisible(false);
			this.createPanel = new FlowPanel();
			Link h = new Link("[Create]");
			h.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Class clazz = ChildBeanWidget.this.objectClass;
					boolean autoSave = GeneralProperties.get().isAutoSave();
					Entity obj = autoSave
							? TransformManager.get().createDomainObject(clazz)
							: TransformManager.get()
									.createProvisionalObject(clazz);
					ClientTransformManager.cast().prepareObject(obj, autoSave,
							true, editable);
					// FIXME - dirndl 1x2 - so many more elegant ways to
					// do this...but...
					// register with the containing savepanel
					//
					// really...'PaneWrapperWithObjects' - let's do this dance
					// again...
					PaneWrapperWithObjects container = WidgetUtils
							.getAncestorWidget(ChildBeanWidget.this,
									PaneWrapperWithObjects.class);
					if (container != null) {
						if (container.getObjects() != null) {
							// provisional
							container.getObjects().add(obj);
						}
					}
					fp.remove(gridForm);
					setValue(obj);
					// forces onattach
					fp.add(gridForm);
				}
			});
			if (editable) {
				createPanel.add(h);
			} else {
				createPanel.add(new Label("[null]"));
			}
			fp.add(createPanel);
			fp.add(gridForm);
			setValue(null);
			initWidget(fp);
		}

		@Override
		public Binding getBinding() {
			return gridForm.getBinding();
		}

		@Override
		public Object getValue() {
			return gridForm.getValue();
		}

		@Override
		public boolean isMultiline() {
			return true;
		}

		@Override
		public void setValue(Object value) {
			Object old = getValue();
			boolean showCreate = value == null;
			createPanel.setVisible(showCreate);
			gridForm.setVisible(!showCreate);
			if (value != null) {
				gridForm.setValue(value);
			}
			if (this.getValue() != old
					&& (this.getValue() == null || (this.getValue() != null
							&& !this.getValue().equals(old)))) {
				this.changes.firePropertyChange("value", old, this.getValue());
			}
		}
	}
}

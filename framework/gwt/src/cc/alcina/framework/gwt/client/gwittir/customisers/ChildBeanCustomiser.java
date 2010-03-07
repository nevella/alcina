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

package cc.alcina.framework.gwt.client.gwittir.customisers;


import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.ide.provider.PropertiesProvider;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.WidgetUtils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.table.Field;
import com.totsp.gwittir.client.ui.table.GridForm;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;

@ClientInstantiable
@SuppressWarnings("unchecked")
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class ChildBeanCustomiser implements Customiser {
	public BoundWidgetProvider getRenderer(boolean editable, Class objectClass,
			boolean multiple, CustomiserInfo info) {
		return new ChildBeanRenderer(editable, objectClass);
	}

	public static class ChildBeanRenderer implements BoundWidgetProvider {
		private final boolean editable;

		private final Class objectClass;

		public ChildBeanRenderer(boolean editable, Class objectClass) {
			this.editable = editable;
			this.objectClass = objectClass;
		}

		public BoundWidget get() {
			return new ChildBeanWidget(objectClass, editable);
		}
	}

	public static class ChildBeanWidget extends AbstractBoundWidget {
		private FlowPanel fp;

		private GridForm gridForm;

		private FlowPanel createPanel;

		private final Class objectClass;

		public ChildBeanWidget(Class objectClass, boolean editable) {
			this.objectClass = objectClass;
			BoundWidgetTypeFactory factory = new BoundWidgetTypeFactory(true);
			Object bean = ClientReflector.get().getTemplateInstance(
					objectClass);
			Field[] fields = GwittirBridge.get()
					.fieldsForReflectedObjectAndSetupWidgetFactory(bean,
							factory, editable, false);
			this.gridForm = new GridForm(fields, 1, factory);
			gridForm.setDirectSetModelDisabled(true);
			// the model should be the child
			// bean(value), not the parent
			gridForm.setStyleName("alcina-ChildBean");
			this.fp = new FlowPanel();
			gridForm.setVisible(false);
			this.createPanel = new FlowPanel();
			Link h = new Link("[Create]");
			h.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					Class clazz = ChildBeanWidget.this.objectClass;
					boolean autoSave = PropertiesProvider
							.getGeneralProperties().isAutoSave();
					Object obj = autoSave ? TransformManager.get()
							.createDomainObject(clazz) : TransformManager.get()
							.createProvisionalObject(clazz);
					// register with the containing savepanel
					PaneWrapperWithObjects container = WidgetUtils
							.getParentWidget(ChildBeanWidget.this,
									PaneWrapperWithObjects.class);
					if (container!=null){
						container.getObjects().add(obj);
					}
					fp.remove(gridForm);
					setValue(obj);
					//forces onattach
					fp.add(gridForm);
				}
			});
			if (editable){
				createPanel.add(h);
			}else{
				createPanel.add(new Label("[null]"));
			}
			fp.add(createPanel);
			fp.add(gridForm);
			setValue(null);
			initWidget(fp);
		}

		public Object getValue() {
			return gridForm.getValue();
		}

		public void setValue(Object value) {
			Object old=getValue();
			boolean showCreate = value == null;
			createPanel.setVisible(showCreate);
			gridForm.setVisible(!showCreate);
			if (value!=null){
				gridForm.setValue(value);
			}
			if( this.getValue() != old && (this.getValue() == null||
	        		(this.getValue() != null && !this.getValue().equals( old ) ))){
	            this.changes.firePropertyChange("value", old, this.getValue());
	        }
		}
	}
}

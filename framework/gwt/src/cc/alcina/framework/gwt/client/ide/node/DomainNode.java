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

package cc.alcina.framework.gwt.client.ide.node;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.HasGeneratedDisplayName;
import cc.alcina.framework.gwt.client.ide.provider.DataImageProvider;
import cc.alcina.framework.gwt.client.ide.widget.DetachListener;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.TreeItem;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class DomainNode<T extends SourcesPropertyChangeEvents> extends FilterableTreeItem
		implements PropertyChangeListener, DetachListener {
	private String displayName;

	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getUserObject() {
		return (T) super.getUserObject();
	}

	public DomainNode(T object) {
		super();
		setUserObject(object);
		ClientBeanReflector info = ClientReflector.get().beanInfoForClass(
				getUserObject().getClass());
		if (object instanceof HasGeneratedDisplayName) {
			object.addPropertyChangeListener(this);
		} else {
			String displayNamePropertyName = info.getGwBeanInfo()
					.displayNamePropertyName();
			Object pv = GwittirBridge.get().getPropertyValue(object,
					displayNamePropertyName);
			if (pv instanceof SourcesPropertyChangeEvents) {
				SourcesPropertyChangeEvents spce = (SourcesPropertyChangeEvents) pv;
				spce.addPropertyChangeListener(this);
			} else {
				object.addPropertyChangeListener(displayNamePropertyName, this);
			}
		}
		refreshFromObject();
	}

	public void removeListeners() {
		T object = getUserObject();
		if (object instanceof HasGeneratedDisplayName){
			return;
		}
		ClientBeanReflector info = ClientReflector.get().beanInfoForClass(
				getUserObject().getClass());
		String displayNamePropertyName = info.getGwBeanInfo()
				.displayNamePropertyName();
		Object pv = GwittirBridge.get().getPropertyValue(object,
				displayNamePropertyName);
		if (pv instanceof SourcesPropertyChangeEvents) {
			SourcesPropertyChangeEvents spce = (SourcesPropertyChangeEvents) pv;
			spce.removePropertyChangeListener(this);
		} else {
			object.removePropertyChangeListener(displayNamePropertyName, this);
		}
	}

	@Override
	public void removeItem(TreeItem item) {
		super.removeItem(item);
		removeListeners();
	}

	protected static DivElement div = Document.get().createDivElement();

	public void refreshFromObject() {
		ClientBeanReflector info = ClientReflector.get().beanInfoForClass(
				getUserObject().getClass());
		displayName = info.getObjectName(getUserObject());
		if (displayName != null) {
			div.setInnerText(displayName);
			displayName = div.getInnerHTML();
		} else {
			displayName = "[null]";
		}
		AbstractImagePrototype img = DataImageProvider.get().getByName(
				info.getGwBeanInfo().displayInfo().iconName());
		setHTML(imageItemHTML(img, displayName));
	}

	protected String imageItemHTML(AbstractImagePrototype imageProto,
			String title) {
		return imageProto.getHTML() + " " + title;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		refreshFromObject();
	}

	public void onDetach() {
		removeListeners();
	}
}

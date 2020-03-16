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

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.TreeItem;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.ClientBeanReflector;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.HasGeneratedDisplayName;
import cc.alcina.framework.gwt.client.ide.widget.DetachListener;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImageProvider;

/**
 * 
 * @author Nick Reddel
 */
public class DomainNode<T extends SourcesPropertyChangeEvents> extends
        FilterableTreeItem implements PropertyChangeListener, DetachListener {
    private String displayName;

    public DomainNode(T object) {
        this(object, null);
    }

    public DomainNode(T object, NodeFactory nodeFactory) {
        super();
        setUserObject(object);
        ClientBeanReflector info = ClientReflector.get()
                .beanInfoForClass(getUserObject().getClass());
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

    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    
    public T getUserObject() {
        return (T) super.getUserObject();
    }

    public void onDetach() {
        removeListeners();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        refreshFromObject();
    }

    public void refreshFromObject() {
        ClientBeanReflector info = ClientReflector.get()
                .beanInfoForClass(getUserObject().getClass());
        displayName = info.getObjectName(getUserObject());
        if (displayName != null) {
            displayName = SafeHtmlUtils.htmlEscape(displayName);
        } else {
            displayName = "[null]";
        }
        if (!isUnrendered()) {
            renderHtml();
        }
    }

    @Override
    protected void renderHtml() {
        ClientBeanReflector info = ClientReflector.get()
                .beanInfoForClass(getUserObject().getClass());
        AbstractImagePrototype img = StandardDataImageProvider.get()
                .getByName(info.getGwBeanInfo().displayInfo().iconName());
        setHTML(imageItemHTML(img, displayName));
    }

    @Override
    public void removeItem(TreeItem item) {
        super.removeItem(item);
        removeListeners();
    }

    public void removeListeners() {
        T object = getUserObject();
        if (object instanceof HasGeneratedDisplayName) {
            return;
        }
        ClientBeanReflector info = ClientReflector.get()
                .beanInfoForClass(getUserObject().getClass());
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
    protected String getText0() {
        return displayName;
    }

    protected String imageItemHTML(AbstractImagePrototype imageProto,
            String title) {
        return imageProto.getHTML() + " " + title;
    }

    @Override
    protected boolean satisfiesFilter(String filterText) {
        T userObject = getUserObject();
        return Registry.impl(HasSatisfiesFilter.class, userObject.getClass())
                .satisfiesFilter(userObject, filterText);
    }

    @RegistryLocation(registryPoint = HasSatisfiesFilter.class, implementationType = ImplementationType.SINGLETON)
    @ClientInstantiable
    public static class DefaultHasSatisfiesFilter<T>
            implements HasSatisfiesFilter<T> {
        @Override
        public boolean satisfiesFilter(T t, String filterText) {
            if (CommonUtils.nullToEmpty(TextProvider.get().getObjectName(t))
                    .toLowerCase().contains(filterText)) {
                return true;
            }
            if (t instanceof HasId) {
                if (filterText.startsWith("id:")) {
                    return String.valueOf(((HasId) t).getId())
                            .equals(filterText.substring(3));
                }
                return String.valueOf(((HasId) t).getId()).equals(filterText);
            }
            return false;
        }
    }
}

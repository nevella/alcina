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

import java.util.Collection;
import java.util.function.Supplier;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.provider.ExpandableDomainNodeCollectionLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.widget.RadioButtonList;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
@ClientInstantiable
public class RadioButtonListCustomiser implements Customiser {
    public static final String SUPPLIER_CLASS = "supplierClass";

    public static final String RENDERER_CLASS = "rendererClass";

    public static final String GROUP_NAME = "groupName";

    public static final String RADIO_BUTTON_STYLE_NAME = "radioButtonStyleName";

    public static final String COLUMN_COUNT = "columnCount";

    @Override
    @SuppressWarnings("unchecked")
    public BoundWidgetProvider getProvider(boolean editable, Class clazz,
            boolean multiple, Custom info) {
        NamedParameter[] parameters = info.parameters();
        if (editable) {
            Supplier supplier = NamedParameter.Support
                    .instantiateClass(parameters, SUPPLIER_CLASS);
            Renderer renderer = NamedParameter.Support
                    .instantiateClass(parameters, RENDERER_CLASS);
            String groupName = NamedParameter.Support.stringValue(parameters,
                    GROUP_NAME, null);
            String radioButtonStyleName = NamedParameter.Support
                    .stringValue(parameters, RADIO_BUTTON_STYLE_NAME, null);
            int columnCount = NamedParameter.Support.intValue(parameters,
                    COLUMN_COUNT, 1);
            return new RadioButtonListProvider(supplier, renderer, groupName,
                    columnCount, radioButtonStyleName);
        } else {
            if (multiple) {
                int maxLength = GwittirBridge.MAX_EXPANDABLE_LABEL_LENGTH;
                return new ExpandableDomainNodeCollectionLabelProvider(
                        maxLength, false);
            } else {
                return GwittirBridge.DN_LABEL_PROVIDER;
            }
        }
    }

    public static class RadioButtonListProvider implements BoundWidgetProvider {
        private Supplier supplier;

        private Renderer renderer;

        private String groupName;

        private int columnCount;

        private String radioButtonStyleName;

        public RadioButtonListProvider(Supplier supplier, Renderer renderer,
                String groupName, int columnCount,
                String radioButtonStyleName) {
            this.supplier = supplier;
            this.renderer = renderer;
            this.groupName = groupName;
            this.columnCount = columnCount;
            this.radioButtonStyleName = radioButtonStyleName;
        }

        @Override
        public BoundWidget get() {
            return new RadioButtonList(groupName, (Collection) supplier.get(),
                    renderer, columnCount, radioButtonStyleName);
        }
    }
}

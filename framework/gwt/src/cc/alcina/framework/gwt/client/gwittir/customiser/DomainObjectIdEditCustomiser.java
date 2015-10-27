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

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.customiser.RenderedLabelCustomiser.RenderedLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.renderer.IdToStringRenderer;
import cc.alcina.framework.gwt.client.gwittir.widget.DomainObjectIdEditor.DomainObjectIdEditorProvider;

import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class DomainObjectIdEditCustomiser implements Customiser {
	public static final String TARGET_CLASS = "targetClass";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		Class classValue = NamedParameter.Support.getParameter(
				info.parameters(), TARGET_CLASS).classValue();
		return editable ? new DomainObjectIdEditorProvider(classValue)
				: new RenderedLabelProvider(IdToStringRenderer.class, null);
	}
}
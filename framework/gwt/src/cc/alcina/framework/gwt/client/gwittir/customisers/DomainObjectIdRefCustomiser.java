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


import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge.DomainObjectIdRefProvider;

import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

@ClientInstantiable
/**
 *
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class DomainObjectIdRefCustomiser implements Customiser {
	public static final String TARGET_CLASS = "targetClass";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, CustomiserInfo info) {
		return new DomainObjectIdRefProvider(NamedParameter.Support.getParameter(
				info.parameters(), TARGET_CLASS).classValue());
	}
}
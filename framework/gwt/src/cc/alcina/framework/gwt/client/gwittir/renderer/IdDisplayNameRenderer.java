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
package cc.alcina.framework.gwt.client.gwittir.renderer;

import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;

/**
 *
 * @author Nick Reddel
 */
@ClientInstantiable
 public class IdDisplayNameRenderer extends FlexibleToStringRenderer<HasId> {
	public static final IdDisplayNameRenderer INSTANCE = new IdDisplayNameRenderer();

	public String render(HasId hasId) {
		if (hasId == null) {
			return "(Undefined)";
		}
		String dn = ClientReflector.get().displayNameForObject(hasId);
		return hasId.getId()+" : "+((dn == null) ? super.render(hasId) : dn);
	}
}
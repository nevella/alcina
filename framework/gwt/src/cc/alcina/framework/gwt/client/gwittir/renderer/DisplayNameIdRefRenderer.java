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

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * 
 * @author Nick Reddel
 */
public class DisplayNameIdRefRenderer extends FlexibleToStringRenderer {
	private Class targetClass;

	public DisplayNameIdRefRenderer(Class targetClass) {
		this.targetClass = targetClass;
	}

	public String render(Object o) {
		if (o == null) {
			return "0";
		}
		Long id = (Long) o;
		HasIdAndLocalId object = Reflections.objectLookup()
				.getObject(targetClass, id, 0);
		String dn = null;
		if (object != null) {
			return ClientReflector.get().displayNameForObject(object);
		} else {
			DisplayNameIdRefResolver resolver = Registry
					.implOrNull(DisplayNameIdRefResolver.class);
			if (resolver != null) {
				return resolver.resolveName(targetClass, id);
			} else {
				return "(" + id + ")";
			}
		}
	}

	public static interface DisplayNameIdRefResolver {
		public String resolveName(Class clazz, long id);
	}
}
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

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.HasDisplayName;

/**
 *
 * @author Nick Reddel
 */
public class DisplayNameIdRefRenderer extends FlexibleToStringRenderer {
	private Class targetClass;

	public DisplayNameIdRefRenderer(Class targetClass) {
		this.targetClass = targetClass;
	}

	@Override
	public String render(Object o) {
		if (o == null) {
			return "";
		}
		Long id = (Long) o;
		Entity object = Domain.find(targetClass, id);
		String dn = null;
		if (object != null) {
			return HasDisplayName.displayName(object);
		} else {
			return Registry.optional(DisplayNameIdRefResolver.class)
					.map(resolver -> resolver.resolveName(targetClass, id))
					.orElseGet(() -> "(" + id + ")");
		}
	}

	public static interface DisplayNameIdRefResolver {
		public String resolveName(Class clazz, long id);
	}
}
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
package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

// TODO - make flat-serializable when needed
@TypeSerialization(flatSerializable = false)
public class EnumCriteriaGroup extends CriteriaGroup<EnumCriterion> {
	public EnumCriteriaGroup() {
		super();
	}

	@Override
	public Class entityClass() {
		return null;
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		if (getCriteria().isEmpty()) {
			return "";
		}
		return getCriteria().iterator().next().getDisplayName();
	}

	@Override
	public String validatePermissions() {
		try {
			for (EnumCriterion ec : getCriteria()) {
				ec.toString();
			}
		} catch (Exception e) {
			return "Access not permitted: (not enum criterion)";
		}
		// either subclass, or rely on property mappings
		return null;
	}
}

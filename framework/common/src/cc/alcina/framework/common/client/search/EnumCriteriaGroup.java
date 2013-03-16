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

import cc.alcina.framework.common.client.logic.reflection.BeanInfo;

@BeanInfo(displayNamePropertyName = "displayName")
/**
 *
 * @author Nick Reddel
 */
public class EnumCriteriaGroup extends CriteriaGroup<EnumCriterion> {
	public EnumCriteriaGroup() {
		super();
		setDisplayName("");
	}

	@Override
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
		return null;// either subclass, or rely on property mappings
	}

	@Override
	public CriteriaGroup clone() throws CloneNotSupportedException {
		return new EnumCriteriaGroup().deepCopy(this);
	}
}
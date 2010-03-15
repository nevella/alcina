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
 * @author <a href="mailto:nick@alcina.cc">Nick Reddel</a>
 */

 public class LongCriteriaGroup extends CriteriaGroup<LongCriterion> {
	public LongCriteriaGroup() {
		super();
		setDisplayName("Long");
	}

	public LongCriteriaGroup(String propertyName, String displayName) {
		this();
		LongCriterion lc = new LongCriterion();
		lc.setTargetPropertyName(propertyName);
		lc.setDisplayName(displayName);
		setDisplayName(displayName);
		getCriteria().add(lc);
	}
}
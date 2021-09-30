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

import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.permissions.PermissibleChildClasses;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

@Bean
/**
 *
 * @author Nick Reddel
 */
@PermissibleChildClasses({ LongCriterion.class })
// TODO - make flat-serializable when needed
@TypeSerialization(flatSerializable = false)
public class LongCriteriaGroup extends CriteriaGroup<LongCriterion> {
	

	private String displayName = "Long";

	public LongCriteriaGroup() {
		super();
		setCombinator(FilterCombinator.OR);
	}

	public LongCriteriaGroup(String displayName) {
		this();
		LongCriterion lc = new LongCriterion();
		lc.setDisplayName(displayName);
		setDisplayName(displayName);
		getCriteria().add(lc);
	}

	@Override
	@AlcinaTransient
	public String getDisplayName() {
		return this.displayName;
	}

	@Override
	public Class entityClass() {
		return null;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}

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

import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 * 
 * 
 */
public abstract class EnumMultipleCriterion<E extends Enum>
		extends SearchCriterion implements HasValue<Set<E>> {
	static final transient long serialVersionUID = -1L;

	public EnumMultipleCriterion() {
	}

	public abstract Class<E> enumClass();

	public boolean equivalentTo(SearchCriterion other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		EnumMultipleCriterion otherImpl = (EnumMultipleCriterion) other;
		return otherImpl.getDirection() == getDirection()
				&& otherImpl.getValue().equals(getValue());
	}

	/**
	 * If the enum is serialised in the db as a string, set to true
	 */
	protected boolean valueAsString() {
		return false;
	}

	public EnumMultipleCriterion(String criteriaDisplayName) {
		super(criteriaDisplayName);
	}

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		Set<E> value = getValue();
		if (value.size() > 0
				&& !CommonUtils.isNullOrEmpty(getTargetPropertyName())) {
			result.eql = targetPropertyNameWithTable() + " in ? ";
			Set params = value;
			if (valueAsString()) {
				params = value.stream().map(Object::toString)
						.collect(Collectors.toSet());
			}
			result.parameters.add(params);
		}
		return result;
	}

	@XmlTransient
	public abstract Set<E> getValue();

	/**
	 * add property change firing to the subclass implementation, if you care
	 */
	public abstract void setValue(Set<E> value);

	@Override
	protected EnumMultipleCriterion
			copyPropertiesFrom(SearchCriterion searchCriterion) {
		EnumMultipleCriterion copyFromCriterion = (EnumMultipleCriterion) searchCriterion;
		return super.copyPropertiesFrom(copyFromCriterion);
	}

	@Override
	public String toString() {
		return String.valueOf(getValue());
	}
}
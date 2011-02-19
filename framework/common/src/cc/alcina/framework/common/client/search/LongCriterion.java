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

import cc.alcina.framework.common.client.util.CommonUtils;


/**
 * 
 * @author Nick Reddel
 */
public class LongCriterion extends SearchCriterion {
	private Long value;

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (value == null) {
			return result;
		}
		result.eql = "t." + getTargetPropertyName() + " =  ? ";
		result.parameters.add(value);
		return result;
	}
	@Override
	public String toString() {
		return value == null ? "":
				value.toString();
	}

	public void setValue(Long value) {
		this.value = value;
	}
	public boolean equivalentTo(SearchCriterion other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		LongCriterion otherImpl = (LongCriterion) other;
		return otherImpl.getDirection() == getDirection()
				&& CommonUtils.equalsWithNullEquality(getValue(), otherImpl
						.getValue());
	}

	public Long getValue() {
		return value;
	}
}
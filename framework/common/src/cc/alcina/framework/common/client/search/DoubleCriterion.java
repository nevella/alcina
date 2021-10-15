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

import cc.alcina.framework.common.client.logic.domain.HasValue;

/**
 * 
 * @author Nick Reddel
 */
public class DoubleCriterion extends SearchCriterion
		implements HasValue<Double> {
	

	private Double value;

	public DoubleCriterion() {
	}

	public DoubleCriterion(Double value) {
		setValue(value);
	}

	@Override
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		if (value == null) {
			return result;
		}
		result.eql = targetPropertyNameWithTable() + " =  ? ";
		result.parameters.add(value);
		return result;
	}

	public Double getDouble() {
		return getValue();
	}

	public Double getValue() {
		return value;
	}

	public void setDouble(Double value) {
		setValue(value);
	}

	public void setValue(Double value) {
		Double old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	@Override
	public String toString() {
		return value == null ? "" : value.toString();
	}
}

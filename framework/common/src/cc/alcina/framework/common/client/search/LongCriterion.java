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
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;

/**
 * 
 * @author Nick Reddel
 */
@TypeSerialization("longvalue")
public class LongCriterion extends SearchCriterion implements HasValue<Long> {
	private Long value;

	public LongCriterion() {
	}

	public LongCriterion(Long value) {
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

	@PropertySerialization(ignore = true)
	public Long getLong() {
		return getValue();
	}

	@Override
	public Long getValue() {
		return value;
	}

	public void setLong(Long value) {
		setValue(value);
	}

	@Override
	public void setValue(Long value) {
		Long old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	@Override
	public String toString() {
		return value == null ? "" : value.toString();
	}

	public <T extends LongCriterion> T withValue(Long value) {
		setValue(value);
		return (T) this;
	}
}

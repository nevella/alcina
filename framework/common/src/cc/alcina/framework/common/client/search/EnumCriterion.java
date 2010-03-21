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

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * 
 * @author Nick Reddel
 */
public abstract class EnumCriterion<E extends Enum> extends SearchCriterion
		implements HasWithNull {
	private boolean withNull;

	public EnumCriterion() {
	}

	/**
	 * If the enum is serialised in the db as a string, set to true
	 */
	protected boolean valueAsString() {
		return false;
	}

	public EnumCriterion(String propertyName, String criteriaDisplayName,
			boolean withNull) {
		super(criteriaDisplayName, propertyName);
		this.withNull = withNull;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		E value = getValue();
		if (value != null
				&& !CommonUtils.isNullOrEmpty(getTargetPropertyName())) {
			result.eql = "t." + getTargetPropertyName() + " = ? ";
			result.parameters.add(valueAsString() ? value.toString() : value);
		}
		return result;
	}

	@XmlTransient
	public abstract E getValue();

	public abstract void setValue(E value);

	public void setWithNull(boolean withNull) {
		this.withNull = withNull;
	}

	public boolean isWithNull() {
		return withNull;
	}
}
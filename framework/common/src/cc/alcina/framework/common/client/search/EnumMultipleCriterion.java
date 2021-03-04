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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.logic.domain.HasElementType;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.serializer.flat.PropertySerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/**
 * 
 * @author Nick Reddel
 * 
 * 
 */
public abstract class EnumMultipleCriterion<E extends Enum>
		extends SearchCriterion implements HasValue<Set<E>>, HasElementType {
	static final transient long serialVersionUID = -1L;

	public EnumMultipleCriterion() {
		setOperator(StandardSearchOperator.CONTAINS);
	}

	public EnumMultipleCriterion(String criteriaDisplayName) {
		super(criteriaDisplayName);
		setOperator(StandardSearchOperator.CONTAINS);
	}

	public abstract Class<E> enumClass();

	@Override
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

	@Override
	@XmlTransient
	@JsonIgnore
	@PropertySerialization(defaultProperty = true)
	public abstract Set<E> getValue();

	@Override
	public Class<?> provideElementType() {
		return enumClass();
	}

	/**
	 * add property change firing to the subclass implementation, if you care
	 */
	@Override
	public abstract void setValue(Set<E> value);

	@Override
	public String toString() {
		return Ax.format("%s: %s", enumClass().getSimpleName(), getValue());
	}

	public <T extends EnumMultipleCriterion<E>> T withValue(E addEnum) {
		Set<E> newValue = new LinkedHashSet<>(getValue());
		newValue.add(addEnum);
		setValue(newValue);
		return (T) this;
	}

	public <T extends EnumMultipleCriterion<E>> T
			withValues(Collection<E> addEnums) {
		Set<E> newValue = new LinkedHashSet<>(getValue());
		newValue.addAll(addEnums);
		setValue(newValue);
		return (T) this;
	}

	public <T extends EnumMultipleCriterion<E>> T withValues(E... addEnums) {
		return withValues(Arrays.asList(addEnums));
	}

	/**
	 * If the enum is serialised in the db as a string, set to true
	 */
	protected boolean valueAsString() {
		return false;
	}
}

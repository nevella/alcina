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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;

/**
 * 
 * @author Nick Reddel
 * 
 * 
 */
public abstract class IdMultipleCriterion<E extends HasIdAndLocalId>
		extends SearchCriterion implements HasValue<Set<E>> {
	static final transient long serialVersionUID = -1L;

	private Set<Long> ids = new LinkedHashSet<>();

	public Set<Long> getIds() {
		return this.ids;
	}

	public void setIds(Set<Long> ids) {
		this.ids = ids;
	}

	private transient Set<E> value;

	public IdMultipleCriterion() {
	}

	public abstract Function<Long, E> objectSupplier();

	public boolean equivalentTo(SearchCriterion other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		IdMultipleCriterion otherImpl = (IdMultipleCriterion) other;
		return otherImpl.getDirection() == getDirection()
				&& otherImpl.getValue().equals(getValue());
	}

	public IdMultipleCriterion(String criteriaDisplayName) {
		super(criteriaDisplayName);
	}

	@AlcinaTransient
	@XmlTransient
	public Set<E> getValue() {
		if (value == null) {
			Function<Long, E> supplier = objectSupplier();
			value = ids.stream().map(id -> supplier.apply(id))
					.collect(Collectors.toSet());
		}
		return value;
	}

	/**
	 * add property change firing to the subclass implementation, if you care
	 */
	public void setValue(Set<E> value) {
		setIds(HiliHelper.toIdSet(value));
		Set<E> old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

	@Override
	protected IdMultipleCriterion
			copyPropertiesFrom(SearchCriterion searchCriterion) {
		IdMultipleCriterion copyFromCriterion = (IdMultipleCriterion) searchCriterion;
		ids = new LinkedHashSet<>(copyFromCriterion.ids);
		return super.copyPropertiesFrom(copyFromCriterion);
	}

	@Override
	public String toString() {
		return String.valueOf(getValue());
	}
}
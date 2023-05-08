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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlTransient;

import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.domain.DomainFilter;
import cc.alcina.framework.common.client.domain.search.DomainCriterionFilter;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.renderer.FriendlyEnumRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/**
 *
 * @author Nick Reddel
 *
 *
 */
public abstract class EnumMultipleCriterion<E extends Enum>
		extends SearchCriterion implements HasValue<Set<E>> {
	private Set<E> value = new LinkedHashSet<>();

	public EnumMultipleCriterion() {
		setOperator(StandardSearchOperator.CONTAINS);
	}

	public EnumMultipleCriterion(String criteriaDisplayName) {
		super(criteriaDisplayName);
		setOperator(StandardSearchOperator.CONTAINS);
	}

	public EnumMultipleCriterion<E> add(E e) {
		getValue().add(e);
		return this;
	}

	public EnumMultipleCriterion<E> add(Set<E> e) {
		getValue().addAll(e);
		return this;
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
	@PropertySerialization(defaultProperty = true)
	@XmlTransient
	public Set<E> getValue() {
		return this.value;
	}

	@Override
	public void setValue(Set<E> value) {
		Set<E> old_value = this.value;
		this.value = value;
		propertyChangeSupport().firePropertyChange("value", old_value, value);
	}

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

	public interface Handler<T, E extends Enum, SC extends EnumMultipleCriterion<E>>
			extends DomainCriterionFilter<SC> {
		public boolean test(T t, Set<E> value);

		@Override
		default DomainFilter getFilter(SC sc) {
			Set<E> values = sc.getValue();
			if (values.isEmpty()) {
				return null;
			}
			Predicate<T> pred = t -> test(t, values);
			return new DomainFilter(pred).invertIf(sc
					.getOperator() == StandardSearchOperator.DOES_NOT_CONTAIN);
		}
	}

	public static abstract class Searchable<E extends Enum, C extends EnumMultipleCriterion<E>>
			extends FlatSearchable<C> {
		protected Class<E> enumClass;

		protected int maxSelectedItems = 999;

		public Searchable(Class<C> clazz, Class<E> enumClass, String objectName,
				String criteriaName) {
			super(clazz, objectName, criteriaName,
					Arrays.asList(StandardSearchOperator.CONTAINS,
							StandardSearchOperator.DOES_NOT_CONTAIN));
			this.enumClass = enumClass;
		}

		@Override
		public AbstractBoundWidget createEditor() {
			return new FlatSearchSelector(enumClass, maxSelectedItems,
					FriendlyEnumRenderer.INSTANCE,
					() -> Arrays.asList(enumClass.getEnumConstants()));
		}

		@Override
		public String getCriterionPropertyName() {
			return "value";
		}

		@Override
		public boolean hasValue(C sc) {
			return sc.getValue() != null && sc.getValue().size() > 0;
		}

		protected <T extends EnumMultipleCriterion.Searchable> T
				maxSelectedItems(int maxSelectedItems) {
			this.maxSelectedItems = maxSelectedItems;
			return (T) this;
		}
	}
}

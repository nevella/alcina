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

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName.PreferDisplayNameRenderer;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/**
 *
 *
 * <p>
 * Note that serialization will require type info, either by overriding
 * get/setValue with covariant types, or via
 * TypeSerialization/PropertySerialization annotations
 *
 * FIXME - dirndl 1x3 - revisit this javadoc (particularly in light of
 * BaseEnumCriterion). 'Basically', just having a value property would be a lot
 * simpler...
 *
 * @author Nick Reddel
 */
public abstract class EnumCriterion<E extends Enum> extends SearchCriterion
		implements HasWithNull, HasValue<E> {
	private boolean withNull = true;

	public EnumCriterion() {
		setOperator(StandardSearchOperator.EQUALS);
	}

	public EnumCriterion(String criteriaDisplayName, boolean withNull) {
		super(criteriaDisplayName);
		setOperator(StandardSearchOperator.EQUALS);
		this.withNull = withNull;
	}

	@Override
	public EqlWithParameters eql() {
		EqlWithParameters result = new EqlWithParameters();
		E value = getValue();
		if (value != null
				&& !CommonUtils.isNullOrEmpty(getTargetPropertyName())) {
			result.eql = targetPropertyNameWithTable() + " = ? ";
			result.parameters.add(valueAsString() ? value.toString() : value);
		}
		return result;
	}

	@Override
	@XmlTransient
	@JsonIgnore
	public abstract E getValue();

	@Override
	public boolean isWithNull() {
		return withNull;
	}

	/**
	 * add property change firing to the subclass implementation, if you care
	 */
	@Override
	public abstract void setValue(E value);

	public void setWithNull(boolean withNull) {
		this.withNull = withNull;
	}

	@Override
	public String toString() {
		return String.valueOf(getValue());
	}

	/**
	 * If the enum is serialised in the db as a string, set to true
	 */
	protected boolean valueAsString() {
		return false;
	}

	public <T extends EnumCriterion<E>> T withValue(E value) {
		setValue(value);
		return (T) this;
	}

	public static abstract class Searchable<E extends Enum, C extends EnumCriterion<E>>
			extends FlatSearchable<C> {
		protected Class<E> enumClass;

		protected int maxSelectedItems = 999;

		public Searchable(Class<C> clazz, Class<E> enumClass, String objectName,
				String criteriaName) {
			super(clazz, objectName, criteriaName,
					Arrays.asList(StandardSearchOperator.EQUALS));
			this.enumClass = enumClass;
		}

		@Override
		public AbstractBoundWidget createEditor() {
			return new FlatSearchSelector(enumClass, 1,
					PreferDisplayNameRenderer.INSTANCE,
					() -> Arrays.asList(enumClass.getEnumConstants()));
		}

		@Override
		public String getCriterionPropertyName() {
			return "value";
		}

		@Override
		public boolean hasValue(C sc) {
			return sc.getValue() != null;
		}
	}
}

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
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.TypedProperty;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;
import cc.alcina.framework.gwt.client.ide.provider.CollectionProvider;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/**
 * <p>
 * This could possibly be better named 'SearchPart' - since a 'criteria' can be
 * 'ordering' or 'transforming' - as well as the default 'filtering'
 * 
 * <p>
 * The reason for this is: search is a pipeline, and a SearchDefinition has
 * strong parallels to a SQL query. It makes more sense <i>for the most part</i>
 * to classify a part of the Definition with the properties of the part, rather
 * than which parent (Definition) field it belongs to.
 * 
 * <p>
 * Note that CriteriaGroup was a mistake (turns out), better (and planned) would
 * be LogicalFilteringCriteria (ALL_OF|ANY_OF|NOT)
 * 
 * <p>
 * TODO - search - targetpropertyname etc should not be part of the api (and
 * particularly should not be fields, rather hard-coded)
 */
@Bean
@Display.AllProperties
@ObjectPermissions(
	read = @Permission(access = AccessLevel.EVERYONE),
	write = @Permission(access = AccessLevel.EVERYONE))
@Registration(JaxbContextRegistration.class)
@TypedProperties
public abstract class SearchCriterion extends Bindable
		implements TreeRenderable, HasReflectiveEquivalence<SearchCriterion>,
		HasEquivalence<SearchCriterion>, TreeSerializable {
	public InstanceProperty<?, ?> valueProperty() {
		TypedProperty<?, ?> typedProperty = new TypedProperty<>(getClass(),
				"value");
		return new InstanceProperty(this, typedProperty);
	}

	/**
	 * Can also apply to things like date criteria, not just order - so leave
	 * here rather than in OrderCriterion
	 *
	 * 
	 */
	@Reflected
	public enum Direction {
		ASCENDING, DESCENDING;

		public static Direction valueOfAbbrev(String string) {
			if (string == null) {
				return null;
			}
			switch (string.toLowerCase()) {
			case "asc":
				return ASCENDING;
			case "desc":
				return DESCENDING;
			default:
				return null;
			}
		}

		public String toAbbrevString() {
			switch (this) {
			case ASCENDING:
				return "asc";
			case DESCENDING:
				return "desc";
			default:
				throw new UnsupportedOperationException();
			}
		}

		public int toComparatorMultiplier() {
			switch (this) {
			case ASCENDING:
				return 1;
			case DESCENDING:
				return -1;
			default:
				throw new UnsupportedOperationException();
			}
		}

		public static Direction ofAscending(boolean ascending) {
			return ascending ? ASCENDING : DESCENDING;
		}

		public Direction reverse() {
			switch (this) {
			case ASCENDING:
				return DESCENDING;
			case DESCENDING:
				return ASCENDING;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	public static final transient String CONTEXT_ENSURE_DISPLAY_NAME = SearchCriterion.class
			+ ".CONTEXT_ENSURE_DISPLAY_NAME";

	// TODO: great big injection hole here - should be checked server-side
	// FIXED: - transient, and set in the server validation phase
	private transient String targetPropertyName;

	private String displayName;

	private StandardSearchOperator operator;

	public SearchCriterion() {
	}

	public SearchCriterion(String displayName) {
		this.displayName = displayName;
	}

	//
	public PackageProperties._SearchCriterion.InstanceProperties
			searchCriterionProperties() {
		return PackageProperties.searchCriterion.instance(this);
	}

	public void addToCriteriaGroup(CriteriaGroup group) {
		group.addCriterion(this);
	}

	public void addToSoleCriteriaGroup(SearchDefinition def) {
		def.addCriterionToSoleCriteriaGroup(this);
	}

	public CollectionProvider collectionProvider() {
		return null;
	}

	public boolean emptyCriterion() {
		if ((this instanceof HasValue)) {
			Object value = ((HasValue) this).getValue();
			if (value instanceof Collection) {
				return ((Collection) value).isEmpty();
			} else {
				return value == null;
			}
		}
		return false;
	}

	public EqlWithParameters eql() {
		return null;
	}

	@Override
	public int equivalenceHash() {
		return Objects.hash(toString());
	}

	@Override
	@AlcinaTransient
	@HasReflectiveEquivalence.Ignore
	public String getDisplayName() {
		if (CommonUtils.isNullOrEmpty(displayName)
				&& LooseContext.is(CONTEXT_ENSURE_DISPLAY_NAME)) {
			return CommonUtils.simpleClassName(getClass());
		}
		return this.displayName;
	}

	@PropertySerialization(path = "op")
	public StandardSearchOperator getOperator() {
		return this.operator;
	}

	@AlcinaTransient
	@XmlTransient
	public String getTargetPropertyName() {
		return targetPropertyName;
	}

	public boolean provideIsFilteringCriterion() {
		return true;
	}

	public boolean provideIsTransformingCriterion() {
		return false;
	}

	public boolean provideIsOrderingCriterion() {
		return false;
	}

	public String provideValueAsRenderableText() {
		return toString();
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setOperator(StandardSearchOperator operator) {
		StandardSearchOperator old_operator = this.operator;
		this.operator = operator;
		propertyChangeSupport().firePropertyChange("operator", old_operator,
				operator);
	}

	public void setTargetPropertyName(String propertyName) {
		this.targetPropertyName = propertyName;
	}

	public String toHtml() {
		return toString();
	}

	public SearchCriterion withOperator(StandardSearchOperator operator) {
		setOperator(operator);
		return this;
	}

	protected String targetPropertyNameWithTable() {
		String targetPropertyName = getTargetPropertyName();
		if (targetPropertyName == null || targetPropertyName.contains(".")) {
			return targetPropertyName;
		}
		return "t." + targetPropertyName;
	}

	@Property.Not
	@XmlTransient
	@JsonIgnore
	public List<StandardSearchOperator> getApplicableOperators() {
		return List.of(StandardSearchOperator.EQUALS);
	}
}

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager.CollectionModificationType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LightSet;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasPermissionsValidation;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;

@Bean(displayNamePropertyName = "displayName")
@RegistryLocation(registryPoint = JaxbContextRegistration.class)
public abstract class CriteriaGroup<SC extends SearchCriterion> extends Bindable
		implements TreeRenderable, Permissible, HasPermissionsValidation,
		HasReflectiveEquivalence<CriteriaGroup> {
	static final transient long serialVersionUID = -1L;

	private FilterCombinator combinator = FilterCombinator.AND;

	private Set<SC> criteria = new LightSet<SC>();

	public CriteriaGroup() {
	}

	@Override
	public AccessLevel accessLevel() {
		return AccessLevel.EVERYONE;
	}

	public void addCriterion(SC criterion) {
		Set<SC> deltaSet = TransformManager.getDeltaSet(criteria, criterion,
				CollectionModificationType.ADD);
		setCriteria(deltaSet);
	}

	public String asString(boolean withGroupName, boolean asHtml) {
		if (provideIsEmpty()) {
			return "";
		}
		String displayName = provideDisplayNamePrefix(withGroupName);
		String result = "";
		int ct = 0;
		Set<String> duplicateDisplayTextCriterionSet = new HashSet<String>();
		for (SC searchCriterion : criteria) {
			String scString = asHtml ? searchCriterion.toHtml()
					: searchCriterion.toString();
			if (duplicateDisplayTextCriterionSet.contains(scString)) {
				continue;
			}
			duplicateDisplayTextCriterionSet.add(scString);
			if (scString != null && scString.length() > 0) {
				if (ct++ != 0) {
					result += " " + combinatorString() + " ";
				}
				result += scString;
				if (searchCriterion instanceof SelfNamingCriterion) {
					displayName = "";
				}
			}
		}
		return result.length() == 0 ? result : displayName + result;
	}

	public <S extends SearchCriterion> S ensureCriterion(S criterion) {
		for (SC sc : getCriteria()) {
			if (sc.getClass() == criterion.getClass()) {
				return (S) sc;
			}
		}
		addCriterion((SC) criterion);
		return criterion;
	}

	/*
	 * only used for single-table search, compiled out for client
	 */
	public EqlWithParameters eql() {
		EqlWithParameters ewp = new EqlWithParameters();
		if (criteria.size() == 0) {
			return ewp;
		}
		StringBuffer sb = new StringBuffer();
		int ct = 0;
		for (SearchCriterion searchCriterion : criteria) {
			EqlWithParameters ewp2 = searchCriterion.eql();
			ewp.parameters.addAll(ewp2.parameters);
			if (CommonUtils.isNullOrEmpty(ewp2.eql)) {
				continue;
			}
			if (ct++ == 0) {
				sb.append("(");
			} else {
				sb.append(" " + combinator.toString() + " ");
			}
			sb.append(ewp2.eql);
		}
		if (ct != 0) {
			sb.append(")");
		}
		ewp.eql = sb.toString();
		return ewp;
	}

	public <S extends SearchCriterion> List<S> findCriteria(Class<S> clazz) {
		List<S> result = new ArrayList<S>();
		for (SC sc : getCriteria()) {
			if (sc.getClass() == clazz) {
				result.add((S) sc);
			}
		}
		return result;
	}

	public <S extends SearchCriterion> S findCriterion(Class<S> clazz) {
		for (SC sc : getCriteria()) {
			if (sc.getClass() == clazz) {
				return (S) sc;
			}
		}
		return null;
	}

	public FilterCombinator getCombinator() {
		return combinator;
	}

	public Set<SC> getCriteria() {
		return this.criteria;
	}

	@Override
	public abstract String getDisplayName();

	@XmlTransient
	@JsonIgnore
	public abstract Class getEntityClass();

	/**
	 * To disallow injection attacks here, the criteria/property name mapping
	 * only happens server-side
	 */
	public void map(Class<? extends SearchCriterion> scClass,
			String propertyName) {
		for (SC sc : getCriteria()) {
			if (sc.getClass() == scClass) {
				sc.setTargetPropertyName(propertyName);
			}
		}
	}

	public boolean provideIsEmpty() {
		for (SearchCriterion criterion : getCriteria()) {
			if (criterion instanceof HasValue
					&& ((HasValue) criterion).getValue() == null) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	public void removeCriterion(SearchCriterion criterion) {
		criteria.remove(criterion);
	}

	@Override
	public String rule() {
		return "";
	}

	public void setCombinator(FilterCombinator combinator) {
		this.combinator = combinator;
	}

	public void setCriteria(Set<SC> criteria) {
		Set<SC> old_criteria = this.criteria;
		this.criteria = criteria;
		propertyChangeSupport().firePropertyChange("criteria", old_criteria,
				criteria);
	}

	public <S extends SearchCriterion> S soleCriterion() {
		return criteria.isEmpty() ? null : (S) criteria.iterator().next();
	}

	public String toHtml() {
		return asString(true, true);
	}

	public void toSoleCriterion(SC criterion) {
		criteria.clear();
		addCriterion(criterion);
	}

	@Override
	public String toString() {
		return asString(true, false);
	}

	@Override
	public String validatePermissions() {
		if (!PermissionsManager.get().isPermissible(this)) {
			// won't be used in search anyway
			return null;
		}
		return DefaultValidation.validatePermissions(this, getCriteria());
	}

	protected String combinatorString() {
		return combinator.toString().toLowerCase();
	}

	protected String provideDisplayNamePrefix(boolean withGroupName) {
		return CommonUtils.isNullOrEmpty(getDisplayName()) || !withGroupName
				? ""
				: CommonUtils.pluralise(
						CommonUtils.capitaliseFirst(getDisplayName()), criteria)
						+ ": ";
	}
}

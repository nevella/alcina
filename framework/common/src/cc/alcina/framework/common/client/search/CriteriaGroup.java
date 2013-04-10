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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.logic.FilterCombinator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.HasPermissionsValidation;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasEquivalence;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;

@BeanInfo(displayNamePropertyName = "displayName")
@RegistryLocation(registryPoint = JaxbContextRegistration.class)
/**
 *
 * @author Nick Reddel
 */
public abstract class CriteriaGroup<SC extends SearchCriterion> extends
		BaseBindable implements TreeRenderable, Permissible,
		HasPermissionsValidation, HasEquivalence<CriteriaGroup> {
	static final transient long serialVersionUID = -1L;
	private transient String displayName;

	private FilterCombinator combinator = FilterCombinator.AND;

	private Set<SC> criteria = new LinkedHashSet<SC>();

	private transient Class entityClass;

	public CriteriaGroup() {
	}

	//
	// @Override
	// public boolean equals(Object obj) {
	// if (obj != null && obj.getClass() == getClass()) {
	// CriteriaGroup cg = (CriteriaGroup) obj;
	// return criteria.equals(cg.criteria) && combinator == cg.combinator;
	// }
	// return super.equals(obj);
	// }
	//
	// Duh
	// @Override
	// public int hashCode() {
	// int h = getClass().hashCode() ^ combinator.hashCode();
	// for (SC c : criteria) {
	// h ^= c.hashCode();
	// }
	// return h;
	// }
	public boolean equivalentTo(CriteriaGroup other) {
		if (other == null || other.getClass() != getClass()
				|| other.getEntityClass() != getEntityClass()
				|| other.getCombinator() != getCombinator()
				|| other.getCriteria().size() != getCriteria().size()) {
			return false;
		}
		List<SC> otherCriteria = new ArrayList<SC>(other.getCriteria());
		for (SC sc : getCriteria()) {
			boolean foundEquiv = false;
			for (SC otherCriterion : otherCriteria) {
				if (sc.equivalentTo(otherCriterion)) {
					otherCriteria.remove(otherCriterion);
					foundEquiv = true;
					break;
				}
			}
			if (!foundEquiv) {
				return false;
			}
		}
		return true;
	}

	public boolean provideIsEmpty() {
		return getCriteria().isEmpty();
	}

	public void addCriterion(SC criterion) {
		criteria.add(criterion);
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

	public <S extends SearchCriterion> S findCriterion(Class<S> clazz) {
		for (SC sc : getCriteria()) {
			if (sc.getClass() == clazz) {
				return (S) sc;
			}
		}
		return null;
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

	/*
	 * only used for single-table search, compiled out for client
	 */
	@SuppressWarnings("unchecked")
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

	public FilterCombinator getCombinator() {
		return combinator;
	}

	public Set<SC> getCriteria() {
		return this.criteria;
	}

	public String getDisplayName() {
		return displayName;
	}

	@XmlTransient
	public Class getEntityClass() {
		return this.entityClass;
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

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setEntityClass(Class entityClass) {
		this.entityClass = entityClass;
	}

	public String toHtml() {
		return asString(true, true);
	}

	@SuppressWarnings("unchecked")
	public <S extends SearchCriterion> S soleCriterion() {
		return criteria.isEmpty()?null:(S) criteria.iterator().next();
	}

	public void toSoleCriterion(SC criterion) {
		criteria.clear();
		criteria.add(criterion);
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

	protected String provideDisplayNamePrefix(boolean withGroupName) {
		return CommonUtils.isNullOrEmpty(getDisplayName()) || !withGroupName ? ""
				: CommonUtils
						.pluralise(
								CommonUtils.capitaliseFirst(getDisplayName()),
								criteria)
						+ ": ";
	}

	protected String combinatorString() {
		return combinator.toString().toLowerCase();
	}

	public String toString() {
		return asString(true, false);
	}

	public AccessLevel accessLevel() {
		return AccessLevel.EVERYONE;
	}

	public String rule() {
		return "";
	}

	protected <T extends CriteriaGroup> T deepCopy(T cg) throws CloneNotSupportedException{
		combinator=cg.combinator ;
		displayName=cg.displayName;
		entityClass=cg.entityClass;
		criteria.clear();
		Set<SC> cgCriteria = cg.getCriteria();
		for (SC sc : cgCriteria) {
			criteria.add((SC) sc.clone());
		}
		return cg;
	}

	public CriteriaGroup clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public String validatePermissions() {
		if (!PermissionsManager.get().isPermissible(this)) {
			return null;// won't be used in search anyway
		}
		return DefaultValidation.validatePermissions(this, getCriteria());
	}

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
}

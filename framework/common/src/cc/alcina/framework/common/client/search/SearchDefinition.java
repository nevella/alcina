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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.entity.WrapperPersistable;
import cc.alcina.framework.common.client.logic.permissions.HasPermissionsValidation;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.publication.ContentDefinition;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.objecttree.TreeRenderable;

//
@RegistryLocation(registryPoint = JaxbContextRegistration.class)
/**
 *
 * @author Nick Reddel
 */
public abstract class SearchDefinition extends WrapperPersistable implements
		Serializable, TreeRenderable, ContentDefinition,
		HasPermissionsValidation, HasEquivalence<SearchDefinition> {
	transient final String orderJoin = ", ";

	private int resultsPerPage;

	private String publicationType;

	private String name;

	private String orderName;

	private int charWidth;

	private int clientSearchIndex;

	private transient Map<Class<? extends CriteriaGroup>, CriteriaGroup> cgs = new HashMap<Class<? extends CriteriaGroup>, CriteriaGroup>();

	private transient Map<Class<? extends OrderGroup>, OrderGroup> ogs = new HashMap<Class<? extends OrderGroup>, OrderGroup>();

	private Set<CriteriaGroup> criteriaGroups = new LinkedHashSet<CriteriaGroup>();

	private Set<OrderGroup> orderGroups = new LinkedHashSet<OrderGroup>();

	protected transient String defaultFilterDescription = "";

	protected transient Map<String, String> propertyColumnAliases = new HashMap<String, String>();

	public static final transient int LARGE_SEARCH = 0xFF0000;

	@SuppressWarnings("unchecked")
	public <C extends CriteriaGroup> C criteriaGroup(Class<C> clazz) {
		return (C) cgs.get(clazz);
	}

	@SuppressWarnings("unchecked")
	public EqlWithParameters eql(boolean withOrderClause) {
		EqlWithParameters ewp = new EqlWithParameters();
		if (criteriaGroups.size() == 0) {
			if (withOrderClause) {
				ewp.eql = orderEql();
			}
			return ewp;
		}
		StringBuffer sb = new StringBuffer();
		sb.append("\n WHERE ");
		int ct = 0;
		for (CriteriaGroup cg : getCriteriaGroups()) {
			if (!PermissionsManager.get().isPermissible(cg)) {
				continue;
			}
			EqlWithParameters ewp2 = cg.eql();
			if (CommonUtils.isNullOrEmpty(ewp2.eql)) {
				continue;
			}
			if (ct++ != 0) {
				sb.append(" AND ");
			}
			sb.append(ewp2.eql);
			ewp.parameters.addAll(ewp2.parameters);
		}
		if (withOrderClause) {
			sb.append(orderEql());
		}
		ewp.eql = sb.toString();
		return ewp;
	}

	public boolean equivalentTo(SearchDefinition other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		List<CriteriaGroup> otherCriteriaGroups = new ArrayList<CriteriaGroup>(
				other.getCriteriaGroups());
		for (CriteriaGroup cg : getCriteriaGroups()) {
			boolean foundEquiv = false;
			for (CriteriaGroup otherCg : otherCriteriaGroups) {
				if (cg.equivalentTo(otherCg)) {
					otherCriteriaGroups.remove(otherCg);
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

	public String filterDescription(boolean html) {
		StringBuffer result = new StringBuffer();
		for (CriteriaGroup criteriaGroup : criteriaGroups) {
			String s = html ? criteriaGroup.toHtml() : criteriaGroup.toString();
			if (!CommonUtils.isNullOrEmpty(s)) {
				if (result.length() != 0) {
					result.append("; ");
				}
				result.append(s);
			}
		}
		return (result.length() == 0) ? defaultFilterDescription : result
				.toString();
	}

	public int getCharWidth() {
		return this.charWidth;
	}

	public int getClientSearchIndex() {
		return this.clientSearchIndex;
	}

	public Set<CriteriaGroup> getCriteriaGroups() {
		return this.criteriaGroups;
	}

	public String getDisplayName() {
		return "";
	}

	public String getName() {
		return name;
	}

	public Set<OrderGroup> getOrderGroups() {
		return this.orderGroups;
	}

	/**
	 * Note - there's a slight risk of "injection" here...if truly concerned,
	 * subclass validatePermissions()
	 * 
	 * @return the property name for "order by" in an eql query, if any
	 */
	public String getOrderName() {
		return this.orderName;
	}

	public String getPublicationType() {
		return publicationType;
	}

	public int getResultsPerPage() {
		return this.resultsPerPage;
	}

	public String idEqlPrefix() {
		return null;
	}

	public String orderDescription(boolean html) {
		StringBuffer result = new StringBuffer();
		for (OrderGroup orderGroup : orderGroups) {
			String s = (html ? orderGroup.toHtml() : orderGroup.toString())
					.trim();
			if (!CommonUtils.isNullOrEmpty(s)) {
				if (result.length() != 0) {
					result.append(", ");
				}
				result.append(s);
			}
		}
		return result.toString();
	}

	@SuppressWarnings("unchecked")
	public <C extends OrderGroup> C orderGroup(Class<C> clazz) {
		return (C) ogs.get(clazz);
	}

	public String propertyAlias(String propertyName) {
		if (propertyColumnAliases.containsKey(propertyName)) {
			return propertyColumnAliases.get(propertyName);
		}
		return propertyName;
	}

	public void resetLookups() {
		cgs = new HashMap<Class<? extends CriteriaGroup>, CriteriaGroup>();
		ogs = new HashMap<Class<? extends OrderGroup>, OrderGroup>();
		for (CriteriaGroup cg : criteriaGroups) {
			cgs.put(cg.getClass(), cg);
		}
		for (OrderGroup og : orderGroups) {
			ogs.put(og.getClass(), og);
		}
	}

	public String resultEqlPrefix() {
		return null;
	}

	public void setCharWidth(int charWidth) {
		this.charWidth = charWidth;
	}

	public void setClientSearchIndex(int clientSearchIndex) {
		this.clientSearchIndex = clientSearchIndex;
	}

	public void setCriteriaGroups(Set<CriteriaGroup> criteriaGroups) {
		this.criteriaGroups = criteriaGroups;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOrderGroups(Set<OrderGroup> orderGroups) {
		this.orderGroups = orderGroups;
	}

	public void setOrderName(String orderName) {
		this.orderName = orderName;
	}

	public void setPublicationType(String publicationType) {
		this.publicationType = publicationType;
	}

	public void setResultsPerPage(int resultsPerPage) {
		this.resultsPerPage = resultsPerPage;
	}

	public String toHtml() {
		return CommonUtils.format("%3%1 - %2", filterDescription(true),
				orderDescription(true),
				CommonUtils.isNullOrEmpty(getName()) ? "" : "<b>" + getName()
						+ "</b> - ");
	}

	@Override
	public String toString() {
		return CommonUtils.format("%1 - %2", filterDescription(false),
				orderDescription(false));
	}

	protected String orderEql() {
		return "";
	}

	protected void putCriteriaGroup(CriteriaGroup cg) {
		cgs.put(cg.getClass(), cg);
		criteriaGroups.add(cg);
	}

	protected void putOrderGroup(OrderGroup og) {
		ogs.put(og.getClass(), og);
		orderGroups.add(og);
	}

	public String validatePermissions() {
		resetLookups();
		mapCriteriaToPropertyNames();
		List<CriteriaGroup> children = new ArrayList<CriteriaGroup>();
		children.addAll(getCriteriaGroups());
		children.addAll(getOrderGroups());
		return DefaultValidation.validatePermissions(this, children);
	}

	public void mapCriteriaToPropertyNames() {
		CriterionPropertyNameMappings crMappings = CommonLocator.get()
				.classLookup().getAnnotationForClass(getClass(),
						CriterionPropertyNameMappings.class);
		if (crMappings != null) {
			for (CriterionPropertyNameMapping mapping : crMappings.value()) {
				criteriaGroup(mapping.criteriaGroupClass()).map(
						mapping.criterionClass(), mapping.propertyName());
			}
		}
	}
}
